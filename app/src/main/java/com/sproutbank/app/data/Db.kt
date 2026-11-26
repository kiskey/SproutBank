package com.sproutbank.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ChildEntity::class, LedgerEntity::class, SpendRequestEntity::class,
                ChoreEntity::class, ChoreCompletionEntity::class,
                SettingsEntity::class, CelebrationEntity::class],
    version = 5, exportSchema = false,
)
abstract class SproutDb : RoomDatabase() {
    abstract fun children(): ChildDao
    abstract fun ledger(): LedgerDao
    abstract fun requests(): SpendRequestDao
    abstract fun chores(): ChoreDao
    abstract fun completions(): ChoreCompletionDao
    abstract fun settings(): SettingsDao
    abstract fun celebrations(): CelebrationDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN goalName TEXT")
                db.execSQL("ALTER TABLE children ADD COLUMN goalEmoji TEXT NOT NULL DEFAULT '🎯'")
                db.execSQL("ALTER TABLE children ADD COLUMN goalTargetCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE children ADD COLUMN goalEtfSymbol TEXT NOT NULL DEFAULT 'VOO'")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN avatarBg TEXT NOT NULL DEFAULT '#DDF5EC'")
                db.execSQL("ALTER TABLE children ADD COLUMN avatarRing TEXT NOT NULL DEFAULT '#0D9488'")
                db.execSQL("ALTER TABLE settings ADD COLUMN parentPin TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "childId INTEGER NOT NULL, title TEXT NOT NULL, emoji TEXT NOT NULL, " +
                    "rewardCents INTEGER NOT NULL, rewardPoints INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chore_completions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, choreId INTEGER NOT NULL, " +
                    "childId INTEGER NOT NULL, submittedAtMs INTEGER NOT NULL, " +
                    "status TEXT NOT NULL, decidedAtMs INTEGER, decisionNote TEXT)"
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ledger ADD COLUMN transactionId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ledger ADD COLUMN account TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("ALTER TABLE ledger ADD COLUMN reversesId INTEGER")
                db.execSQL("ALTER TABLE ledger ADD COLUMN idempotencyKey TEXT")
                db.execSQL("ALTER TABLE ledger ADD COLUMN sourceRequestId INTEGER")
                db.execSQL("ALTER TABLE ledger ADD COLUMN sourceCompletionId INTEGER")

                // Backfill legacy rows.
                db.execSQL("UPDATE ledger SET transactionId = 'legacy:' || id")
                db.execSQL("UPDATE ledger SET idempotencyKey = 'legacy:' || id")
                db.execSQL("UPDATE ledger SET account = CASE WHEN kind = 'POINTS' THEN 'POINTS' ELSE 'CASH' END")
                db.execSQL("UPDATE ledger SET kind = 'CREDIT' WHERE kind = 'POINTS'")
                // Convert spend/redeem to signed negatives.
                db.execSQL("UPDATE ledger SET amountCents = -amountCents WHERE kind IN ('SPEND', 'REDEEM')")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_transactionId` ON `ledger` (`transactionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_idempotencyKey` ON `ledger` (`idempotencyKey`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_reversesId` ON `ledger` (`reversesId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_sourceRequestId_account` ON `ledger` (`sourceRequestId`, `account`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ledger_sourceCompletionId_account` ON `ledger` (`sourceCompletionId`, `account`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ledger_childId_account_timestampMs` ON `ledger` (`childId`, `account`, `timestampMs`)")
            }
        }

        fun build(ctx: Context): SproutDb {
            val db = Room.databaseBuilder(ctx, SproutDb::class.java, "sprout.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
            CoroutineScope(Dispatchers.IO).launch { seed(db) }
            return db
        }

        private suspend fun seed(db: SproutDb) {
            if (db.children().byId(1) == null) {
                db.children().upsert(
                    ChildEntity(
                        id = 1, name = "Mia", emoji = "🦊",
                        avatarBg = "#DDF5EC", avatarRing = "#0D9488",
                        goalName = "PS5", goalEmoji = "🎮",
                        goalTargetCents = 50_000L, goalEtfSymbol = "VOO",
                    )
                )
                db.settings().upsert(SettingsEntity())
                val defaultChores = listOf(
                    ChoreEntity(childId = 1, title = "Make My Bed", emoji = "🛏️", rewardCents = 100, rewardPoints = 0),
                    ChoreEntity(childId = 1, title = "Brush My Teeth", emoji = "🪥", rewardCents = 0, rewardPoints = 10),
                    ChoreEntity(childId = 1, title = "Read a Book", emoji = "📚", rewardCents = 50, rewardPoints = 5),
                    ChoreEntity(childId = 1, title = "Feed the Pet", emoji = "🐾", rewardCents = 75, rewardPoints = 0),
                    ChoreEntity(childId = 1, title = "Water the Plants", emoji = "🪴", rewardCents = 0, rewardPoints = 8),
                )
                defaultChores.forEach { db.chores().insert(it) }
            }
        }
    }
}
