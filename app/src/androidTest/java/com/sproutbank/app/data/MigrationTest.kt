package com.sproutbank.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SproutDb::class.java,
    )

    @Test
    fun migrate2To3_preservesChildrenAndAddsGoalColumns() {
        helper.createDatabase("migration-test-v3", 2).use { db ->
            db.execSQL("INSERT INTO children (id, name, emoji) VALUES (1, 'Mia', '🦊')")
        }
        helper.runMigrationsAndValidate("migration-test-v3", 3, true, SproutDb.MIGRATION_2_3)
            .use { db ->
                db.query("SELECT name, goalEmoji, goalTargetCents, goalEtfSymbol FROM children WHERE id=1")
                    .use { c ->
                        check(c.moveToFirst())
                        check(c.getString(0) == "Mia")
                        check(c.getString(1) == "🎯")
                        check(c.getLong(2) == 0L)
                        check(c.getString(3) == "VOO")
                    }
            }
    }

    @Test
    fun migrate4To5_convertsPointsToCreditsAndSignsAmounts() {
        helper.createDatabase("migration-test-v5", 4).use { db ->
            db.execSQL("INSERT INTO children (id, name, emoji) VALUES (1, 'Mia', '🦊')")
            db.execSQL(
                "INSERT INTO ledger (childId, timestampMs, kind, amountCents, note) VALUES " +
                "(1, 100, 'CREDIT',  500, 'credit')," +
                "(1, 200, 'SPEND',   300, 'spend')," +
                "(1, 300, 'POINTS',  40,  'points')"
            )
        }
        helper.runMigrationsAndValidate("migration-test-v5", 5, true, SproutDb.MIGRATION_4_5)
            .use { db ->
                db.query("SELECT kind, account, amountCents, transactionId FROM ledger ORDER BY id")
                    .use { c ->
                        check(c.moveToFirst())
                        check(c.getString(0) == "CREDIT"); check(c.getString(1) == "CASH")
                        check(c.getLong(2) == 500L); check(c.getString(3) == "legacy:1")
                        check(c.moveToNext())
                        check(c.getString(0) == "SPEND"); check(c.getString(1) == "CASH")
                        check(c.getLong(2) == -300L); check(c.getString(3) == "legacy:2")
                        check(c.moveToNext())
                        check(c.getString(0) == "CREDIT"); check(c.getString(1) == "POINTS")
                        check(c.getLong(2) == 40L); check(c.getString(3) == "legacy:3")
                    }
            }
    }
}




