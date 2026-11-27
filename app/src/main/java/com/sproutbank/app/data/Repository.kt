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

    /** Local-day epoch (midnight) for a millisecond timestamp. */
    fun localDayMs(tsMs: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = tsMs
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
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
    fun childrenAll(): Flow<List<ChildEntity>> = db.children().observeAll()
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

    // ── Kid management (issue 1) ──────────────────────────────────────
    /** Creates a kid. Ids are sequential; ledger FKs stay stable forever. */
    suspend fun addKid(name: String, emoji: String, bg: String, ring: String) =
        db.withTransaction {
            val id = db.children().nextId()
            db.children().upsert(
                ChildEntity(id = id, name = name, emoji = emoji, avatarBg = bg, avatarRing = ring)
            )
        }

    suspend fun saveChild(c: ChildEntity) = db.withTransaction { db.children().upsert(c) }

    /** Archive = hide from UI. Ledger rows and history remain untouched. */
    suspend fun archiveKid(id: Long, archived: Boolean = true) =
        db.withTransaction { db.children().setArchived(id, archived) }

    // ── Config ────────────────────────────────────────────────────────
    suspend fun saveSettings(s: SettingsEntity) = db.settings().upsert(s)
    suspend fun completeOnboarding() = db.withTransaction {
        val cur = db.settings().current() ?: SettingsEntity()
        db.settings().upsert(cur.copy(onboarded = true))
    }

    /**
     * Add or replace a chore atomically (issue 4). A chore edit is a single
     * upsert: assignee list, values and icon all change together or not at
     * all. Pending completions keep pointing at the same chore id — the reward
     * is read at approval time from the *current* template.
     */
    suspend fun saveChore(c: ChoreEntity) = db.withTransaction { db.chores().upsert(c) }
    suspend fun addChore(c: ChoreEntity) = db.withTransaction { db.chores().insert(c) }
    suspend fun deleteChore(c: ChoreEntity) = db.withTransaction { db.chores().delete(c) }

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

    // ── Chore completions (per-date) ──────────────────────────────────
    /**
     * Atomic guard + insert for one kid on one day (issue 2). The UNIQUE
     * (choreId, childId, dateMs) index makes a double submit for the same
     * day impossible even at the DB level; this pre-check just gives a nicer
     * no-op than an exception.
     */
    suspend fun submitChore(chore: ChoreEntity, childId: Long, dateMs: Long = Repository.localDayMs(System.currentTimeMillis())) =
        db.withTransaction {
            val existing = db.completions().forChoreChildDate(chore.id, childId, dateMs)
            if (existing != null) return@withTransaction  // already handled today
            db.completions().insert(ChoreCompletionEntity(
                choreId = chore.id, childId = childId,
                submittedAtMs = System.currentTimeMillis(), status = "PENDING",
                dateMs = dateMs,
            ))
        }

    /**
     * Approves a chore submission:
     *   1. Re-reads completion — throws if not PENDING.
     *   2. Posts CASH/POINTS credit atomically — or a signed DEBIT when the
     *      chore is a deduction (negative reward, issue 10).
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
        val isDeduction = chore.rewardCents < 0 || chore.rewardPoints < 0
        val cashCents = kotlin.math.abs(chore.rewardCents)
        val pointCents = kotlin.math.abs(chore.rewardPoints)
        if (cashCents > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = c.childId,
                account = "CASH",
                kind = if (isDeduction) "PENALTY" else "CREDIT",
                amountCents = if (isDeduction) -cashCents else cashCents,
                timestampMs = ts,
                note = chore.title,
                sourceCompletionId = c.id,
                idempotencyKey = "completion:${c.id}:CASH",
            ))
        }
        if (pointCents > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = c.childId,
                account = "POINTS",
                kind = if (isDeduction) "PENALTY" else "CREDIT",
                amountCents = if (isDeduction) -pointCents else pointCents,
                timestampMs = ts,
                note = chore.title,
                sourceCompletionId = c.id,
                idempotencyKey = "completion:${c.id}:POINTS",
            ))
        }
        db.completions().decide(c.id, "APPROVED", ts, note)
        if (!isDeduction) {
            db.celebrations().insert(CelebrationEntity(
                type = "CREDIT", title = "Chore credited!",
                subtitle = chore.title,
                amountLabel = if (chore.rewardCents > 0) money(chore.rewardCents)
                              else "${chore.rewardPoints} ⭐",
                timestampMs = ts,
            ))
        }
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
     *
     * Issue 8: when the reversed row was sourced by a chore completion, the
     * completion is REOPENed in the same transaction so the kid sees the chore
     * as available again on that same date. Ledger keeps the full audit trail:
     * credit → reversal → (new credit when re-approved).
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
        // Re-availability (issue 8): credit came from a chore completion →
        // reset that day's completion so the chore is doable again.
        original.sourceCompletionId?.let { completionId ->
            db.completions().byId(completionId)?.let { comp ->
                if (comp.status == "APPROVED") {
                    db.completions().decide(
                        comp.id, "REOPEN", System.currentTimeMillis(),
                        "Reopened by reversal: $reason",
                    )
                }
            }
        }
    }

    // ── Backdated credit (issue 9) ─────────────────────────────────────
    /**
     * Parent-only correction for a forgotten chore within the past 30 days.
     * Either unconditional (plain credit) or chore-linked — a chore-linked
     * credit also inserts an APPROVED completion for that date, so the
     * calendar, ledger and kid history all agree.
     *
     * `dateMs` is the local-day epoch; ledger rows carry noon of that day so
     * ordering stays chronological within the day.
     */
    suspend fun backdatedCredit(
        childId: Long,
        dateMs: Long,
        cents: Long,
        points: Long,
        note: String,
        choreId: Long? = null,
    ) = db.withTransaction {
        require(cents > 0 || points > 0) { "Backdated credit must be non-zero" }
        require(kotlin.math.abs(System.currentTimeMillis() - dateMs) <= 30L * 24 * 3600 * 1000 + 24 * 3600 * 1000) {
            "Backdated credits are limited to the past 30 days"
        }
        val noonMs = dateMs + 12 * 3600 * 1000
        val ts = System.currentTimeMillis()
        if (cents > 0) {
            db.ledger().insert(LedgerEntity(
                transactionId = UUID.randomUUID().toString(),
                childId = childId,
                account = "CASH",
                kind = "CREDIT",
                amountCents = cents,
                timestampMs = noonMs,
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
                timestampMs = noonMs,
                note = note,
                idempotencyKey = UUID.randomUUID().toString(),
            ))
        }
        if (choreId != null) {
            val existing = db.completions().forChoreChildDate(choreId, childId, dateMs)
            if (existing == null) {
                db.completions().insert(ChoreCompletionEntity(
                    choreId = choreId, childId = childId,
                    submittedAtMs = noonMs, status = "APPROVED",
                    decidedAtMs = ts, decisionNote = "Backdated credit",
                    dateMs = dateMs,
                ))
            }
        }
        db.celebrations().insert(CelebrationEntity(
            type = "CREDIT", title = "Backdated credit recorded",
            subtitle = note,
            amountLabel = if (cents > 0) money(cents) else "$points ⭐",
            timestampMs = ts,
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
            "CREDIT" -> LedgerKind.Credit
            "BONUS"  -> LedgerKind.Bonus
            "SPEND"  -> LedgerKind.Spend
            else     -> LedgerKind.Redeem
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
