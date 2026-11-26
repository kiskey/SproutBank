package com.sproutbank.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sproutbank.app.data.MarketData                 // ← add this import
import com.sproutbank.app.data.*
import com.sproutbank.app.rust.GoalProgress
import com.sproutbank.app.rust.SpendImpactResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChildUiState(
    val children: List<ChildEntity> = emptyList(),
    val active: ChildEntity? = null,
    val ledger: List<LedgerEntity> = emptyList(),
    val requests: List<SpendRequestEntity> = emptyList(),
    val goal: GoalProgress? = null,
    val balanceCents: Long = 0,
    val pendingCount: Int = 0,
)

class SproutViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SproutRepository(SproutDb.build(app))

    val themeId = repo.settings().map { it?.themeId ?: "meadow_mint" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "meadow_mint")
    val reducedAnimations = repo.settings().map { it?.reducedAnimations ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _activeChildId = MutableStateFlow(1L)
    val activeChildId: StateFlow<Long> = _activeChildId

    val children = repo.children().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ChildUiState> = combine(
        children,
        _activeChildId.flatMapLatest { repo.ledger(it) },
        _activeChildId.flatMapLatest { repo.requests(it) },
        _activeChildId,
    ) { kids, ledger, reqs, activeId ->
        val active = kids.firstOrNull { it.id == activeId }
        val balance = ledger.sumOf { if (it.kind == "CREDIT" || it.kind == "BONUS") it.amountCents else -it.amountCents }
        val goal = active?.let { repo.goalProgress(it, ledger) }
        ChildUiState(
            children = kids, active = active, ledger = ledger, requests = reqs,
            goal = goal, balanceCents = balance,
            pendingCount = reqs.count { it.status == "PENDING" },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChildUiState())

    val allRequests = repo.allRequests().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val latestCelebration = repo.latestCelebration().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── ADD THIS BLOCK HERE ────────────────────────────────────────────
    init {
        viewModelScope.launch {
            MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
        }
    }

    // Optional: expose a public refresh entry point for a "Sync" button
    fun refreshMarkets() = viewModelScope.launch {
        MarketData.trackedEtfs().forEach { runCatching { MarketData.refresh(it) } }
    }
    // ───────────────────────────────────────────────────────────────────

    fun selectTheme(id: String) = viewModelScope.launch {
        repo.saveSettings(SettingsEntity(themeId = id, reducedAnimations = reducedAnimations.value))
    }
    fun setReducedAnimations(v: Boolean) = viewModelScope.launch {
        repo.saveSettings(SettingsEntity(themeId = themeId.value, reducedAnimations = v))
    }
    fun setActiveChild(id: Long) { _activeChildId.value = id }

    fun recordCredit(kind: String, amountCents: Long, note: String) = viewModelScope.launch {
        repo.recordCredit(activeChildId.value, kind, amountCents, note)
    }
    fun requestSpend(kind: String, amountCents: Long, title: String) = viewModelScope.launch {
        repo.requestSpend(activeChildId.value, kind, amountCents, title)
    }
    fun decideRequest(r: SpendRequestEntity, approved: Boolean, note: String?) =
        viewModelScope.launch { repo.decideRequest(r, approved, note) }

    fun setChildGoal(child: ChildEntity, name: String, emoji: String, targetCents: Long, etf: String) =
        viewModelScope.launch {
            repo.saveChild(child.copy(
                goalName = name, goalEmoji = emoji,
                goalTargetCents = targetCents, goalEtfSymbol = etf,
            ))
        }

    fun spendImpact(etf: String): SpendImpactResult {
        val ledger = uiState.value.ledger
        return repo.spendImpact(activeChildId.value, etf, ledger)
    }
}
