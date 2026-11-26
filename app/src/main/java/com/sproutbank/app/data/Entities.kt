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
    val kind: String,                  // CREDIT, BONUS, SPEND, REDEEM, REVERSAL
    val amountCents: Long,             // signed
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
    val kind: String,                 // SPEND | REDEEM
    val amountCents: Long,            // cents when SPEND, raw points when REDEEM
    val title: String,
    val status: String,               // PENDING | APPROVED | DECLINED
    val decidedAtMs: Long? = null,
    val decisionNote: String? = null,
)

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val emoji: String,
    val rewardCents: Long,
    val rewardPoints: Long,
)

@Entity(tableName = "chore_completions")
data class ChoreCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val childId: Long,
    val submittedAtMs: Long,
    val status: String,               // PENDING | APPROVED | REJECTED
    val decidedAtMs: Long? = null,
    val decisionNote: String? = null,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 1,
    val themeId: String = "meadow_mint",
    val reducedAnimations: Boolean = false,
    val parentPin: String = "",
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
