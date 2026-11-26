package com.sproutbank.app.data

import android.util.Log
import androidx.room.withTransaction
import com.sproutbank.app.rust.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Stateless UI helpers. */
object Repository {
    fun money(cents: Long): String = SproutRepository.money(cents)

    fun requestAmountDisplay(req: SpendRequestEntity): String =
        if (req.kind == "REDEEM") "${req.amountCents} ⭐"
        else SproutRepository.money(req.amountCents)

    fun ledgerAmountDisplay(e: LedgerEntity): String =
        if (e.account == "POINTS") "${e.amountCents} ⭐"
        else SproutRepository.money(e.amountCents)
}

/**
 * Authoritative financial state. Every mutation either commits fully or
 * rolls back — no partial debits, no orphan postings, no silent edits.
 *
 * The unique constraints on the ledger table (idempotencyKey, reversesId,
 * sourceRequestId+account, sourceCompletionId+account) enforce idempotency
 * at the database level even if application logic is bypassed.
 */
class SproutRepository(private val db: SproutDb) {

    // ── Reads ─────────────────────────────────────────────────────────
    fun children(): Flow<List<ChildEntity>> = db.children().observe()
    suspend fun child(id: Long) = db.children().byId(id)

    fun ledger(childId: Long): Flow<List<LedgerEntity>> = db.ledger().observeFor(childId)
    fun requests(childId: Long): Flow<List<SpendRequestEntity>> = db.requests().observeFor(childId)
    fun allRequests(): Flow<List<SpendRequestEntity>> = db.requests().observeAll()
    fun chores(childId: Long): Flow<List<ChoreEntity>> = db.chores().observeFor(childId)
    fun allChores(): Flow<List<ChoreEntity>> = db.chores().observeAll()
    fun completions(childId: Long): Flow<List<ChoreCompletionEntity>> = db.completions().observeFor(childId)
    fun allCompletions(): Flow<List<ChoreCompletionEntity>> = db.completions().observeAll()
    fun settings(): Flow<SettingsEntity?> = db.settings().observe()
    suspend fun currentSettings() = db.settings().current()
    fun latestCelebration(): Flow<CelebrationEntity?> = db.celebrations().observeLatest()

    suspend fun cashBalance(childId: Long): Long = db.ledger().sumFor(childId, "CASH")
    suspend fun pointsBalance(childId: Long): Long = db.ledger().sumFor(childId, "POINTS")

    // ── Config ────────────────────────────────────────────────────────
    suspend fun saveChild(c: ChildEntity) = db.children().upsert(c)
    suspend fun saveSettings(s: SettingsEntity) = db.settings().upsert(s)
    suspend fun addChore(c: ChoreEntity) = db.chores().insert(c)
    suspend fun deleteChore(c: ChoreEntity) = db.chores().delete(c)

    // ── Direct credit (bonus / deposit) ───────────────────────────────
    /**
     * Atomic: ledger entries + celebration are one transaction.
     * Idempotency key is a fresh UUID per invocation, so two taps produce
     * two distinct legitimate credits — the UI is responsible for debouncing.
     */
    suspend fun recordCredit(
        childId: Long,
        kind: String,
        amountCents: Long,
        note: String,
        points: Long = 0,
    ) = db.withTransaction {
        val ts = System.currentTimeMillis()
        if (amountCents > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = childId,
                account = "CASH",
                kind = kind,
                amountCents = amountCents,
                timestampMs = ts,
                note = note,
                idempotencyKey = UUID.randomUUID().toString(),
            ))
        }
        if (points > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = childId,
                account = "POINTS",
                kind = "CREDIT",
                amountCents = points,
                timestampMs = ts,
                note = note,
                idempotencyKey = UUID.randomUUID().toString(),
            ))
        }
        db.celebrations().insert(CelebrationEntity(
            type = kind,
            title = if (kind == "BONUS") "Bonus unlocked!" else "Chore credited!",
            subtitle = note.ifBlank { null },
            amountLabel = if (amountCents > 0) money(amountCents) else "$points ⭐",
            timestampMs = ts,
        ))
    }

    // ── Spend requests ────────────────────────────────────────────────
    suspend fun requestSpend(childId: Long, kind: String, amountCents: Long, title: String) {
        db.requests().insert(SpendRequestEntity(
            childId = childId, createdAtMs = System.currentTimeMillis(),
            kind = kind, amountCents = amountCents, title = title, status = "PENDING",
        ))
    }

    /**
     * Approves a pending request:
     *   1. Re-reads the request — throws if it is no longer PENDING.
     *   2. Checks the relevant account has sufficient balance.
     *   3. Posts exactly one debit (unique index on sourceRequestId+account).
     *   4. Marks the request APPROVED with the decision timestamp.
     * All inside a single DB transaction. Failure rolls everything back.
     */
    suspend fun approveRequest(requestId: Long, note: String?) = db.withTransaction {
        val req = db.requests().byId(requestId)
            ?: error("Request $requestId not found")
        if (req.status != "PENDING") {
            error("Request $requestId already ${req.status}")
        }
        val account = if (req.kind == "REDEEM") "POINTS" else "CASH"
        val balance = db.ledger().sumFor(req.childId, account)
        if (balance < req.amountCents) {
            error("Insufficient $account balance (have $balance, need ${req.amountCents})")
        }
        val ts = System.currentTimeMillis()
        db.ledger().insert(LedgerEntity(
            transactionId = UUID.randomUUID().toString(),
            childId = req.childId,
            account = account,
            kind = req.kind,
            amountCents = -req.amountCents,
            timestampMs = ts,
            note = req.title,
            sourceRequestId = req.id,
            idempotencyKey = "request:${req.id}",
        ))
        db.requests().decide(req.id, "APPROVED", ts, note)
        db.celebrations().insert(CelebrationEntity(
            type = "SPEND", title = "Approved!",
            subtitle = req.title,
            amountLabel = if (account == "POINTS") "${req.amountCents} ⭐" else money(req.amountCents),
            timestampMs = ts,
        ))
    }

    /** Decline is a single statement; still wrapped for symmetry and clarity. */
    suspend fun declineRequest(requestId: Long, note: String?) = db.withTransaction {
        val req = db.requests().byId(requestId)
            ?: error("Request $requestId not found")
        if (req.status != "PENDING") error("Request $requestId already ${req.status}")
        db.requests().decide(req.id, "DECLINED", System.currentTimeMillis(),
            note ?: "Not this time.")
    }

    // ── Chore completions ─────────────────────────────────────────────
    /** Atomic pending-guard + insert; safe against double-tap. */
    suspend fun submitChore(chore: ChoreEntity) = db.withTransaction {
        if (db.completions().pendingFor(chore.id) != null) return@withTransaction
        db.completions().insert(ChoreCompletionEntity(
            choreId = chore.id, childId = chore.childId,
            submittedAtMs = System.currentTimeMillis(), status = "PENDING",
        ))
    }

    /**
     * Approves a chore:
     *   1. Re-reads completion — throws if not PENDING.
     *   2. Posts CASH credit and/or POINTS credit atomically.
     *   3. Marks completion APPROVED.
     * Unique index on sourceCompletionId+account prevents double-credit
     * even if two approval taps race.
     */
    suspend fun approveChore(completionId: Long, note: String?) = db.withTransaction {
        val c = db.completions().byId(completionId)
            ?: error("Completion $completionId not found")
        if (c.status != "PENDING") error("Completion $completionId already ${c.status}")
        val chore = db.chores().byId(c.choreId)
            ?: error("Chore ${c.choreId} not found")
        val ts = System.currentTimeMillis()
        if (chore.rewardCents > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = c.childId,
                account = "CASH",
                kind = "CREDIT",
                amountCents = chore.rewardCents,
                timestampMs = ts,
                note = chore.title,
                sourceCompletionId = c.id,
                idempotencyKey = "completion:${c.id}:CASH",
            ))
        }
        if (chore.rewardPoints > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = c.childId,
                account = "POINTS",
                kind = "CREDIT",
                amountCents = chore.rewardPoints,
                timestampMs = ts,
                note = chore.title,
                sourceCompletionId = c.id,
                idempotencyKey = "completion:${c.id}:POINTS",
            ))
        }
        db.completions().decide(c.id, "APPROVED", ts, note)
        db.celebrations().insert(CelebrationEntity(
            type = "CREDIT", title = "Chore credited!",
            subtitle = chore.title,
            amountLabel = if (chore.rewardCents > 0) money(chore.rewardCents)
                          else "${chore.rewardPoints} ⭐",
            timestampMs = ts,
        ))
    }

    suspend fun rejectChore(completionId: Long, note: String?) = db.withTransaction {
        val c = db.completions().byId(completionId)
            ?: error("Completion $completionId not found")
        if (c.status != "PENDING") error("Completion $completionId already ${c.status}")
        db.completions().decide(c.id, "REJECTED", System.currentTimeMillis(),
            note ?: "Let's try again together.")
    }

    // ── Reversals ─────────────────────────────────────────────────────
    /**
     * Appends a reversing entry. Original row is never touched.
     * Unique index on reversesId enforces idempotency — a second reversal
     * of the same transaction fails at the database level even under race.
     */
    suspend fun reverseTransaction(ledgerId: Long, reason: String) = db.withTransaction {
        val original = db.ledger().byId(ledgerId)
            ?: error("Ledger row $ledgerId not found")
        if (original.kind == "REVERSAL") {
            error("Cannot reverse a reversal (row ${original.id})")
        }
        if (db.ledger().findReversalOf(ledgerId) != null) {
            error("Row $ledgerId already reversed")
        }
        val newBalance = db.ledger().sumFor(original.childId, original.account) - original.amountCents
        if (newBalance < 0) {
            Log.w("SproutBank", "Reversal creates negative ${original.account} balance: $newBalance")
            // Allowed — corrections take precedence over balance floor.
        }
        db.ledger().insert(LedgerEntity(
            transactionId = UUID.randomUUID().toString(),
            childId = original.childId,
            account = original.account,
            kind = "REVERSAL",
            amountCents = -original.amountCents,
            timestampMs = System.currentTimeMillis(),
            note = "Reversal: $reason",
            reversesId = original.id,
            idempotencyKey = "reversal:${original.id}",
        ))
    }

    // ── Reconciliation ────────────────────────────────────────────────
    suspend fun reconcile() = FinancialReconciler(db).reconcile()

    // ── Simulation ────────────────────────────────────────────────────
    /**
     * Only CASH transactions that are neither a reversal nor have been
     * reversed contribute to the simulation. A reversed pair cancels out —
     * excluding both keeps the net cash and the compounding math consistent.
     */
    private fun List<LedgerEntity>.toSimulationEvents(): List<LedgerEvent> {
        val reversedIds = filter { it.reversesId != null }.mapNotNull { it.reversesId }.toSet()
        return filter {
            it.account == "CASH" && it.reversesId == null && it.id !in reversedIds
        }.map { it.toRust() }
    }

    fun portfolioSeries(etf: String, ledger: List<LedgerEntity>): List<PortfolioPoint> =
        buildPortfolioSeries(MarketData.pricesFor(etf), ledger.toSimulationEvents())

    fun spendImpact(etf: String, ledger: List<LedgerEntity>): SpendImpactResult =
        computeSpendImpact(MarketData.pricesFor(etf), ledger.toSimulationEvents())

    fun goalProgress(child: ChildEntity, ledger: List<LedgerEntity>): GoalProgress =
        computeGoalProgress(
            ledger.toSimulationEvents(),
            MarketData.pricesFor(child.goalEtfSymbol),
            child.goalTargetCents,
        )

    private fun LedgerEntity.toRust() = LedgerEvent(
        timestampMs = timestampMs,
        kind = when (kind) {
            "CREDIT" -> LedgerKind.CREDIT
            "BONUS"  -> LedgerKind.BONUS
            "SPEND"  -> LedgerKind.SPEND
            else     -> LedgerKind.REDEEM
        },
        amountCents = kotlin.math.abs(amountCents),
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
