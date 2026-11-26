package com.sproutbank.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface ChildDao {
    @Query("SELECT * FROM children") fun observe(): Flow<List<ChildEntity>>
    @Query("SELECT * FROM children WHERE id = :id") suspend fun byId(id: Long): ChildEntity?
    @Upsert suspend fun upsert(child: ChildEntity)
    @Delete suspend fun delete(child: ChildEntity)
}

@Dao interface LedgerDao {
    @Query("SELECT * FROM ledger WHERE childId = :childId ORDER BY timestampMs") fun observeFor(childId: Long): Flow<List<LedgerEntity>>
    @Insert suspend fun insert(entry: LedgerEntity): Long
}

@Dao interface SpendRequestDao {
    @Query("SELECT * FROM spend_requests WHERE childId = :childId ORDER BY createdAtMs DESC") fun observeFor(childId: Long): Flow<List<SpendRequestEntity>>
    @Query("SELECT * FROM spend_requests ORDER BY createdAtMs DESC") fun observeAll(): Flow<List<SpendRequestEntity>>
    @Insert suspend fun insert(r: SpendRequestEntity): Long
    @Query("UPDATE spend_requests SET status = :status, decidedAtMs = :ts, decisionNote = :note WHERE id = :id")
    suspend fun decide(id: Long, status: String, ts: Long, note: String?)
}

@Dao interface ChoreDao {
    @Query("SELECT * FROM chores WHERE childId = :childId ORDER BY id") fun observeFor(childId: Long): Flow<List<ChoreEntity>>
    @Query("SELECT * FROM chores ORDER BY id") fun observeAll(): Flow<List<ChoreEntity>>
    @Insert suspend fun insert(c: ChoreEntity): Long
    @Delete suspend fun delete(c: ChoreEntity)
}

@Dao interface ChoreCompletionDao {
    @Query("SELECT * FROM chore_completions WHERE childId = :childId ORDER BY submittedAtMs DESC") fun observeFor(childId: Long): Flow<List<ChoreCompletionEntity>>
    @Query("SELECT * FROM chore_completions ORDER BY submittedAtMs DESC") fun observeAll(): Flow<List<ChoreCompletionEntity>>
    @Query("SELECT * FROM chore_completions WHERE choreId = :choreId AND status = 'PENDING' LIMIT 1") suspend fun pendingFor(choreId: Long): ChoreCompletionEntity?
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
    @Query("SELECT * FROM celebrations ORDER BY timestampMs DESC LIMIT 1") fun observeLatest(): Flow<CelebrationEntity?>
    @Insert suspend fun insert(c: CelebrationEntity)
}
