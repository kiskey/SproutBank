package com.sproutbank.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val emoji: String = "🦊",
    val avatarBg: String = "#DDF5EC",
    val avatarRing: String = "#0D9488",
    val goalName: String? = null,
    val goalEmoji: String = "🎯",
    val goalTargetCents: Long = 0L,
    val goalEtfSymbol: String = "VOO",
    /**
     * Soft-delete flag. The ledger is append-only and references childId, so a
     * removed kid is archived (hidden from pickers and profile select) instead of
     * deleted. Balance stays auditable in the ledger forever.
     */
    val isArchived: Boolean = false,
)

/**
 * Append-only ledger.
 *
 * - `amountCents` is signed: positive for credits / reversal-of-spend,
 *   negative for debits / reversal-of-credit.
 * - `account` separates the Cash ledger from the Points ledger. They never mix.
 * - `reversesId` points at the original row when `kind == "REVERSAL"`.
 * - `idempotencyKey` is UNIQUE — protects against duplicate postings from
 *   double taps, retries, or app crashes mid-write.
 * - `sourceRequestId` / `sourceCompletionId` are UNIQUE per account — a single
 *   request can never produce two debits, a single completion two credits.
 *
 * Balance is always `SELECT SUM(amountCents) FROM ledger WHERE childId=? AND account=?`.
 * No screen computes its own balance; every screen reads the repository flow.
 */
@Entity(
    tableName = "ledger",
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["reversesId"], unique = true),
        Index(value = ["sourceRequestId", "account"], unique = true),
        Index(value = ["sourceCompletionId", "account"], unique = true),
        Index(value = ["childId", "account", "timestampMs"]),
    ],
)
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val childId: Long,
    val account: String,               // "CASH" | "POINTS"
    val kind: String,                   // CREDIT, BONUS, SPEND, REDEEM, PENALTY, REVERSAL
    val amountCents: Long,              // signed
    val timestampMs: Long,
    val note: String = "",
    val reversesId: Long? = null,
    val idempotencyKey: String? = null,
    val sourceRequestId: Long? = null,
    val sourceCompletionId: Long? = null,
)

@Entity(tableName = "spend_requests")
data class SpendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val createdAtMs: Long,
    val kind: String,                   // SPEND | REDEEM
    val amountCents: Long,             // cents when SPEND, raw points when REDEEM
    val title: String,
    val status: String,                 // PENDING | APPROVED | DECLINED
    val decidedAtMs: Long? = null,
    val decisionNote: String? = null,
)

/**
 * A chore template. Multi-kid (issue 4): `assigneeIds` is a JSON array of
 * child ids; an empty array means "shared by the whole family" (any kid can do
 * it, first completion per kid per day gets credit).
 * `rewardCents` may be negative for deductions (issue 10) — approval posts a
 * signed debit row of kind PENALTY.
 */
@Entity(tableName = "chores", indices = [Index(value = ["childId"])])
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Legacy single-owner column: kept as the *creator/owner* for migration; assigneeIds governs visibility. */
    val childId: Long,
    val title: String,
    val emoji: String,
    val rewardCents: Long,
    val rewardPoints: Long,
    /** JSON array of child ids this chore is assigned to. "[]" = everyone. */
    val assigneeIds: String = "[]",
    /** DAILY: can be completed once per day. WEEKLY: once per week (per dateMs uniqueness). */
    val cadence: String = "DAILY",
)

/**
 * One submission of a chore by one kid on one local calendar day (issue 2/8).
 * `dateMs` is the local-day epoch (midnight) so "state of date D" is a pure
 * table lookup. UNIQUE(choreId, childId, dateMs) makes a day double-submit
 * impossible at the DB level.
 * REOPEN (issue 8): after a parent reverses a chore credit the same day, the
 * row is flipped to REOPEN — the chore becomes available again on that date
 * while the ledger audit trail (credit → reversal → new credit) is preserved.
 */
@Entity(
    tableName = "chore_completions",
    indices = [Index(value = ["choreId", "childId", "dateMs"], unique = true)],
)
data class ChoreCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val childId: Long,
    val submittedAtMs: Long,
    val status: String,                 // PENDING | APPROVED | REJECTED | REOPEN
    val decidedAtMs: Long? = null,
    val decisionNote: String? = null,
    /** Local-day epoch this completion belongs to (drives per-date UI). */
    val dateMs: Long = 0L,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1,
    val themeId: String = "meadow_mint",
    val reducedAnimations: Boolean = false,
    val parentPin: String = "",
    /** First-launch onboarding (issue 10) completed. */
    val onboarded: Boolean = false,
)

@Entity(tableName = "celebrations")
data class CelebrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val subtitle: String?,
    val amountLabel: String?,
    val timestampMs: Long,
)
