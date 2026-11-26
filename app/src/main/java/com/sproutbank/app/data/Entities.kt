package com.sproutbank.app.data

import androidx.room.Entity
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

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val timestampMs: Long,
    val kind: String,          // CREDIT, BONUS, SPEND, REDEEM, POINTS
    val amountCents: Long,
    val note: String = "",
)

@Entity(tableName = "spend_requests")
data class SpendRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val createdAtMs: Long,
    val kind: String,          // SPEND or REDEEM
    val amountCents: Long,
    val title: String,
    val status: String,        // PENDING, APPROVED, DECLINED
    val decidedAtMs: Long? = null,
    val decisionNote: String? = null,
)

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val emoji: String,
    val rewardCents: Long,     // 0 if points-only
    val rewardPoints: Long,    // 0 if money-only
)

@Entity(tableName = "chore_completions")
data class ChoreCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val choreId: Long,
    val childId: Long,
    val submittedAtMs: Long,
    val status: String,        // PENDING, APPROVED, REJECTED
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
