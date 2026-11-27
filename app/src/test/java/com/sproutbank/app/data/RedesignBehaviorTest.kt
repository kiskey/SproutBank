package com.sproutbank.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the UX-redesign behaviors:
 *   - per-date chore state (issue 2)
 *   - chore re-open after reversal (issue 8)
 *   - backdated credit within 30 days (issue 9)
 *   - multi-kid assignment visibility (issue 4)
 *   - deductions post negative ledger rows (issue 10)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RedesignBehaviorTest {

    private lateinit var db: SproutDb
    private lateinit var repo: SproutRepository
    private val childId = 1L
    private val otherChildId = 2L
    private val today = Repository.localDayMs(System.currentTimeMillis())

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SproutDb::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SproutRepository(db)
        runBlocking {
            db.children().upsert(ChildEntity(id = childId, name = "Mia"))
            db.children().upsert(ChildEntity(id = otherChildId, name = "Leo"))
            db.chores().insert(ChoreEntity(
                id = 100, childId = childId, title = "Test", emoji = "🛏️",
                rewardCents = 500, rewardPoints = 10,
            ))
        }
    }

    @After
    fun tearDown() { db.close() }

    // ── Issue 2: per-date completions ────────────────────────────────

    @Test
    fun doubleSubmit_sameDay_isNoOp() = runBlocking {
        val chore = db.chores().byId(100)!!
        repo.submitChore(chore, childId, today)
        repo.submitChore(chore, childId, today) // second tap same day
        // only one row for (chore, child, day)
        val snapshot = db.completions().observeAll().first()
        assertEquals(1, snapshot.count { it.choreId == 100L && it.dateMs == today })
    }

    @Test
    fun differentDays_bothAllowed() = runBlocking {
        val chore = db.chores().byId(100)!!
        val yesterday = today - 24 * 3600 * 1000L
        repo.submitChore(chore, childId, yesterday)
        repo.submitChore(chore, childId, today)
        val snapshot = db.completions().observeAll().first()
        assertEquals(2, snapshot.count { it.choreId == 100L })
    }

    // ── Issue 8: reversal re-opens the chore ────────────────────────

    @Test
    fun reversal_reopensCompletion_forThatDay() = runBlocking {
        val chore = db.chores().byId(100)!!
        repo.submitChore(chore, childId, today)
        val comp = db.completions().forChoreChildDate(100, childId, today)!!
        repo.approveChore(comp.id, null)
        assertEquals(500L, repo.cashBalance(childId))

        // find the credit row sourced by this completion and reverse it
        val ledger = db.ledger().observeFor(childId).first()
        val credit = ledger.first { it.sourceCompletionId == comp.id && it.account == "CASH" }
        repo.reverseTransaction(credit.id, "mistake")

        val after = db.completions().byId(comp.id)!!
        assertEquals("REOPEN", after.status)
        assertEquals(0L, repo.cashBalance(childId))
    }

    @Test
    fun reopenedCompletion_allowsResubmissionSameDay() = runBlocking {
        val chore = db.chores().byId(100)!!
        repo.submitChore(chore, childId, today)
        val comp = db.completions().forChoreChildDate(100, childId, today)!!
        repo.approveChore(comp.id, null)
        val ledger = db.ledger().observeFor(childId).first()
        repo.reverseTransaction(ledger.first { it.sourceCompletionId == comp.id }.id, "mistake")

        // kid taps the chore again on the same day
        repo.submitChore(chore, childId, today)
        val again = db.completions().forChoreChildDate(100, childId, today)
        assertEquals("REOPEN", again!!.status) // row not duplicated
    }

    // ── Issue 9: backdated credit ───────────────────────────────────

    @Test
    fun backdatedCredit_postsLedgerOnSelectedDay() = runBlocking {
        val date = today - 3L * 24 * 3600 * 1000
        repo.backdatedCredit(childId, date, cents = 250, points = 5, note = "Forgot Friday chore")
        val ledger = db.ledger().observeFor(childId).first()
        val cashRow = ledger.first { it.account == "CASH" }
        assertEquals(250L, cashRow.amountCents)
        assertEquals(Repository.localDayMs(date), Repository.localDayMs(cashRow.timestampMs))
        assertEquals(250L, repo.cashBalance(childId))
        assertEquals(5L, repo.pointsBalance(childId))
    }

    @Test
    fun backdatedCredit_olderThan30Days_throws() = runBlocking {
        val tooOld = today - 40L * 24 * 3600 * 1000
        val err = runCatching { repo.backdatedCredit(childId, tooOld, 100, 0, "nope") }.exceptionOrNull()
        assertTrue(err != null)
        assertEquals(0L, repo.cashBalance(childId))
    }

    @Test
    fun backdatedCredit_linkedChore_createsApprovedCompletion() = runBlocking {
        val date = today - 2L * 24 * 3600 * 1000
        repo.backdatedCredit(childId, date, cents = 500, points = 10, note = "makeup", choreId = 100)
        val comp = db.completions().forChoreChildDate(100, childId, date)
        assertNotNull(comp)
        assertEquals("APPROVED", comp!!.status)
    }

    // ── Issue 4: multi-kid assignment ───────────────────────────────

    @Test
    fun unassignedChore_visibleToAllKids() = runBlocking {
        db.chores().upsert(ChoreEntity(
            id = 200, childId = childId, title = "Family", emoji = "🧹",
            rewardCents = 100, rewardPoints = 0, assigneeIds = "[]",
        ))
        val forMia = db.chores().observeFor(childId).first()
        val forLeo = db.chores().observeFor(otherChildId).first()
        assertTrue(forMia.any { it.id == 200L })
        assertTrue(forLeo.any { it.id == 200L })
    }

    @Test
    fun assignedChore_visibleOnlyToAssignees() = runBlocking {
        db.chores().upsert(ChoreEntity(
            id = 201, childId = childId, title = "Leo only", emoji = "🐕",
            rewardCents = 100, rewardPoints = 0, assigneeIds = "[2]",
        ))
        val forMia = db.chores().observeFor(childId).first()
        val forLeo = db.chores().observeFor(otherChildId).first()
        assertTrue(forMia.none { it.id == 201L })
        assertTrue(forLeo.any { it.id == 201L })
    }

    // ── Issue 10: deductions ────────────────────────────────────────

    @Test
    fun deductionApproval_postsNegativeRow() = runBlocking {
        db.chores().upsert(ChoreEntity(
            id = 300, childId = childId, title = "Extra screen", emoji = "📱",
            rewardCents = -100, rewardPoints = 0,
        ))
        repo.recordCredit(childId, "BONUS", 500, "seed")
        repo.submitChore(db.chores().byId(300)!!, childId, today)
        val comp = db.completions().forChoreChildDate(300, childId, today)!!
        repo.approveChore(comp.id, null)
        val ledger = db.ledger().observeFor(childId).first()
        val penalty = ledger.first { it.kind == "PENALTY" }
        assertEquals(-100L, penalty.amountCents)
        assertEquals(400L, repo.cashBalance(childId))
    }

    // ── Issue 1: kid CRUD / archive keeps ledger ────────────────────

    @Test
    fun archivedKid_ledgerRemains() = runBlocking {
        repo.recordCredit(childId, "BONUS", 900, "seed")
        repo.archiveKid(childId, true)
        val visible = db.children().observe().first()
        assertTrue(visible.none { it.id == childId })
        assertEquals(900L, repo.cashBalance(childId)) // history intact
    }

    @Test
    fun addKid_generatesDistinctIds() = runBlocking {
        repo.addKid("New", "🐼", "#DDF5EC", "#0D9488")
        val all = db.children().observeAll().first()
        assertEquals(3, all.size)
        assertEquals(3L, all.maxOf { it.id })
    }
}
