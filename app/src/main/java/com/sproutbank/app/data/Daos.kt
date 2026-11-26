package com.sproutbank.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class BalanceRow(val childId: Long, val account: String, val total: Long)

@Dao interface ChildDao {
    @Query("SELECT * FROM children") fun observe(): Flow<List<ChildEntity>>
    @Query("SELECT * FROM children WHERE id = :id") suspend fun byId(id: Long): ChildEntity?
    @Upsert suspend fun upsert(child: ChildEntity)
    @Delete suspend fun delete(child: ChildEntity)
}

@Dao interface LedgerDao {
    @Query("SELECT * FROM ledger WHERE childId = :childId ORDER BY timestampMs, id")
    fun observeFor(childId: Long): Flow<List<LedgerEntity>>

    @Insert suspend fun insert(entry: LedgerEntity): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM ledger WHERE childId = :childId AND account = :account")
    suspend fun sumFor(childId: Long, account: String): Long

    @Query("SELECT childId, account, SUM(amountCents) AS total FROM ledger GROUP BY childId, account")
    suspend fun sumAllByAccount(): List<BalanceRow>

    @Query("SELECT * FROM ledger WHERE id = :id") suspend fun byId(id: Long): LedgerEntity?

    @Query("SELECT * FROM ledger WHERE reversesId = :originalId LIMIT 1")
    suspend fun findReversalOf(originalId: Long): LedgerEntity?

    @Query("SELECT * FROM ledger WHERE reversesId IS NOT NULL AND reversesId NOT IN (SELECT id FROM ledger)")
    suspend fun findOrphanReversals(): List<LedgerEntity>

    @Query("SELECT * FROM ledger WHERE sourceRequestId IS NOT NULL AND sourceRequestId IN (" +
           "SELECT sourceRequestId FROM ledger WHERE sourceRequestId IS NOT NULL " +
           "GROUP BY sourceRequestId, account HAVING COUNT(*) > 1)")
    suspend fun findDuplicateRequestPostings(): List<LedgerEntity>

    @Query("SELECT * FROM ledger WHERE sourceCompletionId IS NOT NULL AND sourceCompletionId IN (" +
           "SELECT sourceCompletionId FROM ledger WHERE sourceCompletionId IS NOT NULL " +
           "GROUP BY sourceCompletionId, account HAVING COUNT(*) > 1)")
    suspend fun findDuplicateCompletionPostings(): List<LedgerEntity>
}

@Dao interface SpendRequestDao {
    @Query("SELECT * FROM spend_requests WHERE childId = :childId ORDER BY createdAtMs DESC")
    fun observeFor(childId: Long): Flow<List<SpendRequestEntity>>

    @Query("SELECT * FROM spend_requests ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<SpendRequestEntity>>

    @Query("SELECT * FROM spend_requests WHERE id = :id")
    suspend fun byId(id: Long): SpendRequestEntity?

    @Insert suspend fun insert(r: SpendRequestEntity): Long

    @Query("UPDATE spend_requests SET status = :status, decidedAtMs = :ts, decisionNote = :note WHERE id = :id")
    suspend fun decide(id: Long, status: String, ts: Long, note: String?)
}

@Dao interface ChoreDao {
    @Query("SELECT * FROM chores WHERE childId = :childId ORDER BY id") fun observeFor(childId: Long): Flow<List<ChoreEntity>>
    @Query("SELECT * FROM chores ORDER BY id") fun observeAll(): Flow<List<ChoreEntity>>
    @Query("SELECT * FROM chores WHERE id = :id") suspend fun byId(id: Long): ChoreEntity?
    @Insert suspend fun insert(c: ChoreEntity): Long
    @Delete suspend fun delete(c: ChoreEntity)
}

@Dao interface ChoreCompletionDao {
    @Query("SELECT * FROM chore_completions WHERE childId = :childId ORDER BY submittedAtMs DESC")
    fun observeFor(childId: Long): Flow<List<ChoreCompletionEntity>>

    @Query("SELECT * FROM chore_completions ORDER BY submittedAtMs DESC")
    fun observeAll(): Flow<List<ChoreCompletionEntity>>

    @Query("SELECT * FROM chore_completions WHERE choreId = :choreId AND status = 'PENDING' LIMIT 1")
    suspend fun pendingFor(choreId: Long): ChoreCompletionEntity?

    @Query("SELECT * FROM chore_completions WHERE id = :id")
    suspend fun byId(id: Long): ChoreCompletionEntity?

    @Insert suspend fun insert(c: ChoreCompletionEntity): Long

    @Query("UPDATE chore_completions SET status = :status, decidedAtMs = :ts, decisionNote = :note WHERE id = :id")
    suspend fun decide(id: Long, status: String, ts: Long, note: String?)
}

@Dao interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1") fun observe(): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings WHERE id = 1") suspend fun current(): SettingsEntity?
    @Upsert suspend fun upsert(s: SettingsEntity)
}

@Dao interface CelebrationDao {
    @Query("SELECT * FROM celebrations ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<CelebrationEntity?>
    @Insert suspend fun insert(c: CelebrationEntity)
}
