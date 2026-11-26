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
                SettingsEntity::class, CelebrationEntity::class],
    version = 3, exportSchema = false,
)
abstract class SproutDb : RoomDatabase() {
    abstract fun children(): ChildDao
    abstract fun ledger(): LedgerDao
    abstract fun requests(): SpendRequestDao
    abstract fun settings(): SettingsDao
    abstract fun celebrations(): CelebrationDao

    companion object {
        // Public so androidTest's MigrationTestHelper can reference it.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN goalName TEXT")
                db.execSQL("ALTER TABLE children ADD COLUMN goalEmoji TEXT NOT NULL DEFAULT '🎯'")
                db.execSQL("ALTER TABLE children ADD COLUMN goalTargetCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE children ADD COLUMN goalEtfSymbol TEXT NOT NULL DEFAULT 'VOO'")
            }
        }

        fun build(ctx: Context): SproutDb {
            val db = Room.databaseBuilder(ctx, SproutDb::class.java, "sprout.db")
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
            CoroutineScope(Dispatchers.IO).launch { seed(db) }
            return db
        }

        private suspend fun seed(db: SproutDb) {
            val existing = db.children().byId(1)
            if (existing == null) {
                db.children().upsert(
                    ChildEntity(
                        id = 1, name = "Mia", emoji = "🦊",
                        goalName = "PS5", goalEmoji = "🎮",
                        goalTargetCents = 50_000L, goalEtfSymbol = "VOO",
                    )
                )
                db.settings().upsert(SettingsEntity())
            }
        }
    }
}
