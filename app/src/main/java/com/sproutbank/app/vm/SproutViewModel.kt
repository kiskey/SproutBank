package com.sproutbank.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutbank.app.data.*
import com.sproutbank.app.rust.GoalProgress
import com.sproutbank.app.rust.SpendImpactResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

class SproutViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SproutRepository(SproutDb.build(app))

    val themeId = repo.settings().map { it?.themeId ?: "meadow_mint" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "meadow_mint")
    val reducedAnimations = repo.settings().map { it?.reducedAnimations ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _activeChildId = MutableStateFlow<Long?>(null)
    val activeChildId: StateFlow<Long?> = _activeChildId

    val children = repo.children().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ChildUiState> = combine(
        children,
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.ledger(id)
        },
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.requests(id)
        },
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.chores(id)
        },
        _activeChildId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.completions(id)
        },
        _activeChildId,
    ) { kids, ledger, reqs, chores, completions, activeId ->
        val active = kids.firstOrNull { it.id == activeId }
        val balance = ledger
            .filter { it.kind != "POINTS" }
            .sumOf { if (it.kind == "CREDIT" || it.kind == "BONUS") it.amountCents else -it.amountCents }
        val points = ledger.filter { it.kind == "POINTS" }.sumOf { it.amountCents }
        val goal = active?.let { repo.goalProgress(it, ledger) }
        ChildUiState(
            children = kids, active = active, ledger = ledger, requests = reqs,
            chores = chores, completions = completions,
            goal = goal, balanceCents = balance, points = points,
            pendingCount = reqs.count { it.status == "PENDING" } +
                completions.count { it.status == "PENDING" },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChildUiState())

    val allRequests = repo.allRequests().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allCompletions = repo.allCompletions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allChores = repo.allChores().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val latestCelebration = repo.latestCelebration().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
        }
    }

    fun refreshMarkets() = viewModelScope.launch {
        MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
    }

    fun choreState(chore: ChoreEntity): ChoreUiState {
        val comp = uiState.value.completions.firstOrNull { it.choreId == chore.id }
        return when (comp?.status) {
            "PENDING" -> ChoreUiState.PENDING
            "APPROVED" -> ChoreUiState.DONE
            "REJECTED" -> ChoreUiState.REJECTED
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

    fun recordCredit(kind: String, amountCents: Long, note: String, points: Long = 0) =
        viewModelScope.launch {
            _activeChildId.value?.let { repo.recordCredit(it, kind, amountCents, note, points) }
        }
    fun requestSpend(kind: String, amountCents: Long, title: String) = viewModelScope.launch {
        _activeChildId.value?.let { repo.requestSpend(it, kind, amountCents, title) }
    }
    fun decideRequest(r: SpendRequestEntity, approved: Boolean, note: String?) =
        viewModelScope.launch { repo.decideRequest(r, approved, note) }

    fun submitChore(chore: ChoreEntity) = viewModelScope.launch { repo.submitChore(chore) }
    fun decideChore(c: ChoreCompletionEntity, approved: Boolean, note: String?) =
        viewModelScope.launch {
            val chore = allChores.value.firstOrNull { it.id == c.choreId } ?: return@launch
            repo.decideChore(c, chore, approved, note)
        }
    fun addChore(childId: Long, title: String, emoji: String, cents: Long, points: Long) =
        viewModelScope.launch {
            repo.addChore(ChoreEntity(childId = childId, title = title, emoji = emoji,
                rewardCents = cents, rewardPoints = points))
        }

    fun addBonus(childId: Long, cents: Long, points: Long, note: String) = viewModelScope.launch {
        repo.recordCredit(childId, "BONUS", cents, note.ifBlank { "Bonus" }, points)
    }

    fun choreStateOfDate(state: ChildUiState, date: java.time.LocalDate): ChoreUiState {
        if (date != java.time.LocalDate.now()) return ChoreUiState.AVAILABLE
        return when {
            state.completions.any { it.status == "PENDING" } -> ChoreUiState.PENDING
            state.chores.isNotEmpty() && state.chores.all { c ->
                state.completions.any { it.choreId == c.id && it.status == "APPROVED" }
            } -> ChoreUiState.DONE
            else -> ChoreUiState.AVAILABLE
        }
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
}
