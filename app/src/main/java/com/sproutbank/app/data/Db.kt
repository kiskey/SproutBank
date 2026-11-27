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
    version = 6, exportSchema = false,
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
        /**
         * v5 → v6 (UX redesign):
         *  - children.isArchived (soft delete, ledger stays auditable)
         *  - settings.onboarded (first-launch wizard gate)
         *  - chores.assigneeIds (multi-kid assignment) + chores.cadence
         *  - chore_completions.dateMs + UNIQUE(choreId, childId, dateMs).
         *    Room cannot ALTER a UNIQUE constraint in place, so the table is
         *    recreated and rows copied with dateMs backfilled from submittedAtMs
         *    (local-day epoch).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN onboarded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chores ADD COLUMN assigneeIds TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE chores ADD COLUMN cadence TEXT NOT NULL DEFAULT 'DAILY'")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS chore_completions_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "choreId INTEGER NOT NULL, childId INTEGER NOT NULL, " +
                    "submittedAtMs INTEGER NOT NULL, status TEXT NOT NULL, " +
                    "decidedAtMs INTEGER, decisionNote TEXT, dateMs INTEGER NOT NULL DEFAULT 0)"
                )
                // Local-day epoch backfill: SQLite date functions work in UTC; the
                // boundary drift (≤1 day for extreme timezones) is acceptable for
                // a one-time migration of historical submissions.
                db.execSQL(
                    "INSERT INTO chore_completions_new (id, choreId, childId, submittedAtMs, status, decidedAtMs, decisionNote, dateMs) " +
                    "SELECT id, choreId, childId, submittedAtMs, status, decidedAtMs, decisionNote, " +
                    "CAST(strftime('%s', date(submittedAtMs / 1000, 'unixepoch', 'localtime')) AS INTEGER) * 1000 " +
                    "FROM chore_completions"
                )
                db.execSQL("DROP TABLE chore_completions")
                db.execSQL("ALTER TABLE chore_completions_new RENAME TO chore_completions")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chore_completions_choreId_childId_dateMs` ON `chore_completions` (`choreId`, `childId`, `dateMs`)")
            }
        }

        fun build(ctx: Context): SproutDb {
            val db = Room.databaseBuilder(ctx, SproutDb::class.java, "sprout.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
            CoroutineScope(Dispatchers.IO).launch { seed(db) }
            return db
        }

        /**
         * Seed policy (issue 1 / issue 10): a *fresh install* starts empty and is
         * routed into the onboarding wizard. A *legacy install upgrading* (v5 DB
         * with children already present but no onboarding flag) keeps its demo
         * child and chores and is flagged onboarded so it never sees the wizard.
         */
        private suspend fun seed(db: SproutDb) {
            val settings = db.settings().current()
            if (settings == null) {
                db.settings().upsert(SettingsEntity())
            }
            if (settings != null && settings.onboarded) return

            if (db.children().byId(1) != null) {
                // Upgrade from a pre-redesign install: keep data, skip wizard.
                if (settings != null && !settings.onboarded) {
                    db.settings().upsert(settings.copy(onboarded = true))
                }
                return
            }
            // Fresh install: no demo child, no demo chores — onboarding will
            // create the family, chores, values and PIN.
            if (settings != null && !settings.onboarded && db.children().byId(1) == null) {
                // leave onboarded=false so the wizard shows
            }
        }
    }
}
