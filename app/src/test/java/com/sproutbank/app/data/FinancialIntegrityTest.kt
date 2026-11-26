package com.sproutbank.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FinancialIntegrityTest {

    private lateinit var db: SproutDb
    private lateinit var repo: SproutRepository
    private val childId = 1L

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, SproutDb::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SproutRepository(db)
        runBlocking {
            db.children().upsert(ChildEntity(id = childId, name = "Mia"))
            db.chores().insert(ChoreEntity(
                id = 100, childId = childId, title = "Test", emoji = "🛏️",
                rewardCents = 500, rewardPoints = 10,
            ))
        }
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun doubleApproval_ofSpendRequest_debitsOnce() = runBlocking {
        val id = db.requests().insert(SpendRequestEntity(
            childId = childId, createdAtMs = 0, kind = "SPEND",
            amountCents = 1000, title = "Toy", status = "PENDING",
        ))
        repo.recordCredit(childId, "BONUS", 2000, "seed")
        repo.approveRequest(id, null)
        val failure = runCatching { repo.approveRequest(id, null) }.exceptionOrNull()
        assertTrue("second approve must fail", failure != null)
        assertEquals(1000L, repo.cashBalance(childId))
    }

    @Test
    fun doubleApproval_ofChoreCompletion_creditsOnce() = runBlocking {
        val chore = db.chores().byId(100)!!
        repo.submitChore(chore)
        val c = db.completions().pendingFor(100)!!
        repo.approveChore(c.id, null)
        val second = runCatching { repo.approveChore(c.id, null) }.exceptionOrNull()
        assertTrue(second != null)
        assertEquals(500L, repo.cashBalance(childId))
        assertEquals(10L, repo.pointsBalance(childId))
    }

    @Test
    fun simultaneousApprovals_onlyOneSucceeds() = runBlocking {
        val id = db.requests().insert(SpendRequestEntity(
            childId = childId, createdAtMs = 0, kind = "SPEND",
            amountCents = 1000, title = "Toy", status = "PENDING",
        ))
        repo.recordCredit(childId, "BONUS", 2000, "seed")
        val results = (1..2).map {
            async(Dispatchers.IO) { runCatching { repo.approveRequest(id, null) } }
        }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        assertEquals(1000L, repo.cashBalance(childId))
    }

    @Test
    fun approval_rejectsInsufficientBalance() = runBlocking {
        repo.recordCredit(childId, "BONUS", 500, "seed")
        val id = db.requests().insert(SpendRequestEntity(
            childId = childId, createdAtMs = 0, kind = "SPEND",
            amountCents = 1000, title = "Toy", status = "PENDING",
        ))
        val err = runCatching { repo.approveRequest(id, null) }.exceptionOrNull()
        assertTrue(err is IllegalStateException)
        assertEquals(500L, repo.cashBalance(childId))
        assertEquals("PENDING", db.requests().byId(id)!!.status)
    }

    @Test
    fun duplicateReversal_throws() = runBlocking {
        repo.recordCredit(childId, "BONUS", 1000, "orig")
        val original = db.ledger().byId(1L)!!
        repo.reverseTransaction(original.id, "fix")
        val err = runCatching { repo.reverseTransaction(original.id, "again") }.exceptionOrNull()
        assertTrue(err != null)
        assertEquals(0L, repo.cashBalance(childId))
    }

    @Test
    fun reversalOfReversal_throws() = runBlocking {
        repo.recordCredit(childId, "BONUS", 1000, "orig")
        val original = db.ledger().byId(1L)!!
        repo.reverseTransaction(original.id, "fix")
        val reversal = db.ledger().byId(2L)!!
        val err = runCatching { repo.reverseTransaction(reversal.id, "again") }.exceptionOrNull()
        assertTrue(err != null)
    }

    @Test
    fun redeemingPoints_doesNotTouchCash() = runBlocking {
        repo.recordCredit(childId, "BONUS", 1000, "seed", points = 20)
        assertEquals(1000L, repo.cashBalance(childId))
        assertEquals(20L, repo.pointsBalance(childId))
        val id = db.requests().insert(SpendRequestEntity(
            childId = childId, createdAtMs = 0, kind = "REDEEM",
            amountCents = 15, title = "Sticker", status = "PENDING",
        ))
        repo.approveRequest(id, null)
        assertEquals(1000L, repo.cashBalance(childId))
        assertEquals(5L, repo.pointsBalance(childId))
    }

    @Test
    fun reconciler_detectsOrphanReversal() = runBlocking {
        db.ledger().insert(LedgerEntity(
            transactionId = "orphan",
            childId = childId,
            account = "CASH",
            kind = "REVERSAL",
            amountCents = 500,
            timestampMs = 0,
            reversesId = 9999,
            idempotencyKey = "orphan-1",
        ))
        val report = repo.reconcile()
        assertTrue(report.issues.any { it.contains("Orphan reversal") })
    }

    @Test
    fun reconciler_cleanDatabase_hasNoIssues() = runBlocking {
        repo.recordCredit(childId, "BONUS", 1000, "seed")
        val report = repo.reconcile()
        assertTrue(report.issues.isEmpty())
        assertEquals(1000L, report.cashByChild[childId])
    }
}
