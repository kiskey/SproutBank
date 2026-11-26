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
        helper.createDatabase("migration-test", 2).use { db ->
            db.execSQL(
                "INSERT INTO children (id, name, emoji) VALUES (1, 'Mia', '🦊')"
            )
        }
        helper.runMigrationsAndValidate("migration-test", 3, true, SproutDb.MIGRATION_2_3)
            .use { db ->
                db.query("SELECT name, goalEmoji, goalTargetCents, goalEtfSymbol FROM children WHERE id=1")
                    .use { c ->
                        check(c.moveToFirst()) { "Seed row missing after migration" }
                        check(c.getString(0) == "Mia") { "name lost" }
                        check(c.getString(1) == "🎯") { "goalEmoji default wrong" }
                        check(c.getLong(2) == 0L) { "goalTargetCents default wrong" }
                        check(c.getString(3) == "VOO") { "goalEtfSymbol default wrong" }
                    }
            }
    }
}
