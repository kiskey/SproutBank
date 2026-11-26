package com.sproutbank.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface ChildDao {
    @Query("SELECT * FROM children") fun observe(): Flow<List<ChildEntity>>
    @Query("SELECT * FROM children WHERE id = :id") suspend fun byId(id: Long): ChildEntity?
    @Upsert suspend fun upsert(child: ChildEntity)
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

@Dao interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1") fun observe(): Flow<SettingsEntity?>
    @Upsert suspend fun upsert(s: SettingsEntity)
}

@Dao interface CelebrationDao {
    @Query("SELECT * FROM celebrations ORDER BY timestampMs DESC LIMIT 1") fun observeLatest(): Flow<CelebrationEntity?>
    @Insert suspend fun insert(c: CelebrationEntity)
}
