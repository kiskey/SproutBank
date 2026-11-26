package com.sproutbank.app.data

import com.sproutbank.app.rust.*
import kotlinx.coroutines.flow.Flow

class SproutRepository(private val db: SproutDb) {

    fun children(): Flow<List<ChildEntity>> = db.children().observe()
    suspend fun child(id: Long) = db.children().byId(id)

    fun ledger(childId: Long): Flow<List<LedgerEntity>> = db.ledger().observeFor(childId)
    fun requests(childId: Long): Flow<List<SpendRequestEntity>> = db.requests().observeFor(childId)
    fun allRequests(): Flow<List<SpendRequestEntity>> = db.requests().observeAll()
    fun settings(): Flow<SettingsEntity?> = db.settings().observe()
    fun latestCelebration(): Flow<CelebrationEntity?> = db.celebrations().observeLatest()

    suspend fun saveChild(c: ChildEntity) = db.children().upsert(c)
    suspend fun saveSettings(s: SettingsEntity) = db.settings().upsert(s)

    suspend fun recordCredit(childId: Long, kind: String, amountCents: Long, note: String) {
        val id = db.ledger().insert(
            LedgerEntity(childId = childId, timestampMs = System.currentTimeMillis(),
                kind = kind, amountCents = amountCents, note = note)
        )
        db.celebrations().insert(
            CelebrationEntity(
                type = kind, title = when (kind) {
                    "BONUS" -> "Bonus unlocked!" else -> "Chore credited!"
                },
                subtitle = note.ifBlank { null },
                amountLabel = money(amountCents),
                timestampMs = System.currentTimeMillis(),
            )
        )
    }

    suspend fun requestSpend(childId: Long, kind: String, amountCents: Long, title: String) {
        db.requests().insert(
            SpendRequestEntity(
                childId = childId,
                createdAtMs = System.currentTimeMillis(),
                kind = kind, amountCents = amountCents,
                title = title, status = "PENDING",
            )
        )
    }

    suspend fun decideRequest(r: SpendRequestEntity, approved: Boolean, note: String?) {
        val ts = System.currentTimeMillis()
        if (approved) {
            db.ledger().insert(
                LedgerEntity(
                    childId = r.childId, timestampMs = ts,
                    kind = r.kind, amountCents = r.amountCents,
                    note = r.title,
                )
            )
            db.celebrations().insert(
                CelebrationEntity(
                    type = "SPEND", title = "Approved!",
                    subtitle = r.title, amountLabel = money(r.amountCents), timestampMs = ts,
                )
            )
            db.requests().decide(r.id, "APPROVED", ts, note)
        } else {
            db.requests().decide(r.id, "DECLINED", ts, note ?: "Not this time.")
        }
    }

    // ---- Simulation --------------------------------------------------

    fun portfolioSeries(childId: Long, etf: String, ledger: List<LedgerEntity>): List<PortfolioPoint> {
        val events = ledger.map { it.toRust() }
        return buildPortfolioSeries(MarketData.pricesFor(etf), events)
    }

    fun spendImpact(childId: Long, etf: String, ledger: List<LedgerEntity>): SpendImpactResult {
        val events = ledger.map { it.toRust() }
        return computeSpendImpact(MarketData.pricesFor(etf), events)
    }

    fun goalProgress(child: ChildEntity, ledger: List<LedgerEntity>): GoalProgress {
        val events = ledger.map { it.toRust() }
        return computeGoalProgress(
            events,
            MarketData.pricesFor(child.goalEtfSymbol),
            child.goalTargetCents,
        )
    }

    private fun LedgerEntity.toRust() = LedgerEvent(
        timestampMs = timestampMs,
        kind = when (kind) {
            "CREDIT" -> LedgerKind.CREDIT
            "BONUS"  -> LedgerKind.BONUS
            "SPEND"  -> LedgerKind.SPEND
            else      -> LedgerKind.REDEEM
        },
        amountCents = amountCents,
        note = note,
    )

    companion object {
        fun money(cents: Long): String {
            val sign = if (cents < 0) "-" else ""
            val a = kotlin.math.abs(cents)
            return "%s$%d.%02d".format(sign, a / 100, a % 100)
        }
    }
}
