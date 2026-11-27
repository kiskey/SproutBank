package com.sproutbank.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutbank.app.data.*
import com.sproutbank.app.rust.GoalProgress
import com.sproutbank.app.rust.SpendImpactResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ChoreUiState { AVAILABLE, PENDING, DONE, REJECTED }

data class ChildUiState(
    val children: List<ChildEntity> = emptyList(),
    val active: ChildEntity? = null,
    val ledger: List<LedgerEntity> = emptyList(),
    val requests: List<SpendRequestEntity> = emptyList(),
    val chores: List<ChoreEntity> = emptyList(),
    val completions: List<ChoreCompletionEntity> = emptyList(),
    val goal: GoalProgress? = null,
    val balanceCents: Long = 0,
    val points: Long = 0,
    val pendingCount: Int = 0,
)

/** Internal container so we can combine up to 5 flows (Kotlin limit) into one. */
private data class CoreTriple(
    val kids: List<ChildEntity>,
    val ledger: List<LedgerEntity>,
    val reqs: List<SpendRequestEntity>,
)

class SproutViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SproutRepository(SproutDb.build(app))

    val themeId = repo.settings().map { it?.themeId ?: "meadow_mint" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "meadow_mint")
    val reducedAnimations = repo.settings().map { it?.reducedAnimations ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** Issue 10: gates the onboarding wizard on first launch. */
    val onboarded = repo.settings().map { it?.onboarded ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true /* avoid flashing the wizard */)

    private val _activeChildId = MutableStateFlow<Long?>(null)
    val activeChildId: StateFlow<Long?> = _activeChildId

    val children = repo.children().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val childrenAll = repo.childrenAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // combine() caps at 5 flows — nest the first three into CoreTriple, then
    // combine that with chores, completions, and activeId (4 total).
    val uiState: StateFlow<ChildUiState> = combine(
        combine(
            children,
            _activeChildId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList<LedgerEntity>()) else repo.ledger(id)
            },
            _activeChildId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList<SpendRequestEntity>()) else repo.requests(id)
            },
        ) { kids, ledger, reqs -> CoreTriple(kids, ledger, reqs) },
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList<ChoreEntity>()) else repo.chores(id)
        },
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList<ChoreCompletionEntity>()) else repo.completions(id)
        },
        _activeChildId,
    ) { core, chores, completions, activeId ->
        val active = core.kids.firstOrNull { it.id == activeId }
        val balance = core.ledger.filter { it.account == "CASH" }.sumOf { it.amountCents }
        val points = core.ledger.filter { it.account == "POINTS" }.sumOf { it.amountCents }
        val goal = active?.let { repo.goalProgress(it, core.ledger) }
        ChildUiState(
            children = core.kids, active = active, ledger = core.ledger, requests = core.reqs,
            chores = chores, completions = completions,
            goal = goal, balanceCents = balance, points = points,
            pendingCount = core.reqs.count { it.status == "PENDING" } +
                completions.count { it.status == "PENDING" },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChildUiState())

    val allRequests = repo.allRequests().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allCompletions = repo.allCompletions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allChores = repo.allChores().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Celebrations (issue 5 fix) ────────────────────────────────────
    // The old design surfaced `latestCelebration` (a DB query). Re-entering the
    // kid screen re-subscribed to that flow and replayed the latest row as a
    // "new" toast on every parent↔kid switch. Instead, the UI consumes ONE-SHOT
    // events from this channel: a celebration is shown exactly once, when the
    // underlying mutation happens, never on re-subscription.
    private val _celebrationEvents = MutableSharedFlow<CelebrationEntity>(
        replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val celebrationEvents: SharedFlow<CelebrationEntity> = _celebrationEvents.asSharedFlow()

    init {
        // Bridge: every new celebration row (id > last seen) becomes an event.
        // Snapshot skip on first emission so the initial query result is NOT
        // treated as a fresh toast.
        viewModelScope.launch {
            var lastId = 0L
            repo.latestCelebration().collect { latest ->
                if (latest != null && latest.id > lastId) {
                    // First pass (lastId==0) is the DB snapshot: record the id,
                    // never emit — that is exactly the replay bug (issue 5).
                    if (lastId != 0L) _celebrationEvents.tryEmit(latest)
                    lastId = latest.id
                }
            }
        }
        viewModelScope.launch {
            MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
        }
        viewModelScope.launch {
            runCatching { repo.reconcile() }
                .onFailure { android.util.Log.e("SproutBank", "Reconcile crashed", it) }
        }
    }

    fun refreshMarkets() = viewModelScope.launch {
        MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
    }

    // ── Date-aware chore state (issue 2) ───────────────────────────────
    /** State of one chore for one kid on one calendar day. */
    fun choreState(chore: ChoreEntity, date: LocalDate = LocalDate.now()): ChoreUiState {
        val dayMs = date.toEpochMs()
        val comp = uiState.value.completions.firstOrNull {
            it.choreId == chore.id && it.dateMs == dayMs
        }
        return when (comp?.status) {
            "PENDING" -> ChoreUiState.PENDING
            "APPROVED" -> ChoreUiState.DONE
            "REJECTED" -> ChoreUiState.REJECTED
            "REOPEN" -> ChoreUiState.AVAILABLE        // issue 8: available again
            else -> ChoreUiState.AVAILABLE
        }
    }

    /** Whole-day state for the calendar strip. */
    fun choreStateOfDate(state: ChildUiState, date: LocalDate): ChoreUiState {
        val dayMs = date.toEpochMs()
        val dayComps = state.completions.filter { it.dateMs == dayMs }
        return when {
            dayComps.any { it.status == "PENDING" } -> ChoreUiState.PENDING
            state.chores.isNotEmpty() && state.chores.all { c ->
                dayComps.any { it.choreId == c.id && it.status == "APPROVED" }
            } -> ChoreUiState.DONE
            else -> ChoreUiState.AVAILABLE
        }
    }

    fun enterChild(id: Long) { _activeChildId.value = id }
    fun leaveChild() { _activeChildId.value = null }

    fun selectTheme(id: String) = viewModelScope.launch {
        val cur = repo.currentSettings() ?: SettingsEntity()
        repo.saveSettings(cur.copy(themeId = id))
    }
    fun setReducedAnimations(v: Boolean) = viewModelScope.launch {
        val cur = repo.currentSettings() ?: SettingsEntity()
        repo.saveSettings(cur.copy(reducedAnimations = v))
    }
    fun setParentPin(pin: String) = viewModelScope.launch {
        val cur = repo.currentSettings() ?: SettingsEntity()
        repo.saveSettings(cur.copy(parentPin = pin))
    }
    suspend fun getParentPin(): String = repo.currentSettings()?.parentPin ?: ""

    fun completeOnboarding() = viewModelScope.launch { repo.completeOnboarding() }

    // ── Kid management (issue 1) ───────────────────────────────────────
    fun addKid(name: String, emoji: String, bg: String, ring: String) = viewModelScope.launch {
        runCatching { repo.addKid(name, emoji, bg, ring) }
            .onFailure { android.util.Log.w("SproutBank", "addKid: ${it.message}") }
    }
    fun updateChild(child: ChildEntity) = viewModelScope.launch {
        runCatching { repo.saveChild(child) }
            .onFailure { android.util.Log.w("SproutBank", "saveChild: ${it.message}") }
    }
    fun archiveKid(id: Long, archived: Boolean = true) = viewModelScope.launch {
        runCatching { repo.archiveKid(id, archived) }
            .onFailure { android.util.Log.w("SproutBank", "archiveKid: ${it.message}") }
    }

    fun recordCredit(kind: String, amountCents: Long, note: String, points: Long = 0) =
        viewModelScope.launch {
            _activeChildId.value?.let { repo.recordCredit(it, kind, amountCents, note, points) }
        }

    fun requestSpend(kind: String, amountCents: Long, title: String) = viewModelScope.launch {
        _activeChildId.value?.let { repo.requestSpend(it, kind, amountCents, title) }
    }

    fun approveRequest(r: SpendRequestEntity, note: String? = null) = viewModelScope.launch {
        runCatching { repo.approveRequest(r.id, note) }
            .onFailure { android.util.Log.w("SproutBank", "approveRequest: ${it.message}") }
    }
    fun declineRequest(r: SpendRequestEntity, note: String?) = viewModelScope.launch {
        runCatching { repo.declineRequest(r.id, note) }
            .onFailure { android.util.Log.w("SproutBank", "declineRequest: ${it.message}") }
    }

    fun submitChore(chore: ChoreEntity, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        val childId = _activeChildId.value ?: return@launch
        runCatching { repo.submitChore(chore, childId, date.toEpochMs()) }
            .onFailure { android.util.Log.w("SproutBank", "submitChore: ${it.message}") }
    }
    fun approveChore(c: ChoreCompletionEntity, note: String? = null) = viewModelScope.launch {
        runCatching { repo.approveChore(c.id, note) }
            .onFailure { android.util.Log.w("SproutBank", "approveChore: ${it.message}") }
    }
    fun rejectChore(c: ChoreCompletionEntity, note: String?) = viewModelScope.launch {
        runCatching { repo.rejectChore(c.id, note) }
            .onFailure { android.util.Log.w("SproutBank", "rejectChore: ${it.message}") }
    }

    /** Multi-kid atomic save (issue 4). `assigneeIds` empty = whole family. */
    fun saveChore(chore: ChoreEntity) = viewModelScope.launch {
        runCatching { repo.saveChore(chore) }
            .onFailure { android.util.Log.w("SproutBank", "saveChore: ${it.message}") }
    }
    fun deleteChore(chore: ChoreEntity) = viewModelScope.launch {
        runCatching { repo.deleteChore(chore) }
            .onFailure { android.util.Log.w("SproutBank", "deleteChore: ${it.message}") }
    }

    fun addBonus(childId: Long, cents: Long, points: Long, note: String) = viewModelScope.launch {
        repo.recordCredit(childId, "BONUS", cents, note.ifBlank { "Bonus" }, points)
    }

    /** Issue 9: backdated credit within the past 30 days. */
    fun backdatedCredit(
        childId: Long, date: LocalDate, cents: Long, points: Long, note: String, choreId: Long? = null,
    ) = viewModelScope.launch {
        runCatching { repo.backdatedCredit(childId, date.toEpochMs(), cents, points, note, choreId) }
            .onFailure { android.util.Log.w("SproutBank", "backdatedCredit: ${it.message}") }
    }

    fun setChildGoal(child: ChildEntity, name: String, emoji: String, targetCents: Long, etf: String) =
        viewModelScope.launch {
            repo.saveChild(child.copy(goalName = name, goalEmoji = emoji,
                goalTargetCents = targetCents, goalEtfSymbol = etf))
        }

    fun updateAvatar(child: ChildEntity, emoji: String, bg: String, ring: String) =
        viewModelScope.launch {
            repo.saveChild(child.copy(emoji = emoji, avatarBg = bg, avatarRing = ring))
        }

    fun spendImpact(etf: String): SpendImpactResult =
        repo.spendImpact(etf, uiState.value.ledger)

    fun reverseTransaction(ledgerId: Long, reason: String) = viewModelScope.launch {
        runCatching { repo.reverseTransaction(ledgerId, reason) }
            .onFailure { android.util.Log.w("SproutBank", "reverse: ${it.message}") }
    }

    fun reconcileNow() = viewModelScope.launch {
        val report = repo.reconcile()
        if (!report.isClean) {
            android.util.Log.e("SproutBank", "Integrity issues: ${report.issues.size}")
        }
    }

    companion object {
        /** Local-day epoch for a LocalDate (matches Repository.localDayMs). */
        fun LocalDate.toEpochMs(): Long =
            java.util.Date(
                java.util.Calendar.getInstance().let { cal ->
                    cal.set(year, monthValue - 1, dayOfMonth, 0, 0, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
            ).time
    }
}
