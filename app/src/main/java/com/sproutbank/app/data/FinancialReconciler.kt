package com.sproutbank.app.data

import android.util.Log

/**
 * Verifies stored financial state at startup.
 *
 * Per design: "Flag unrecoverable integrity problems instead of silently
 * correcting them." We never mutate here — we only report.
 */
class FinancialReconciler(private val db: SproutDb) {

    data class Report(
        val cashByChild: Map<Long, Long>,
        val pointsByChild: Map<Long, Long>,
        val issues: List<String>,
    ) {
        val isClean: Boolean get() = issues.isEmpty()
    }

    suspend fun reconcile(): Report {
        val rows = db.ledger().sumAllByAccount()
        val cash = rows.filter { it.account == "CASH" }.associate { it.childId to it.total }
        val points = rows.filter { it.account == "POINTS" }.associate { it.childId to it.total }

        val issues = mutableListOf<String>()

        db.ledger().findOrphanReversals().forEach {
            issues += "Orphan reversal: ledger #${it.id} references missing #${it.reversesId}"
        }
        db.ledger().findDuplicateRequestPostings().forEach {
            issues += "Duplicate posting for request ${it.sourceRequestId} on ${it.account}"
        }
        db.ledger().findDuplicateCompletionPostings().forEach {
            issues += "Duplicate posting for completion ${it.sourceCompletionId} on ${it.account}"
        }

        if (issues.isNotEmpty()) {
            issues.forEach { Log.e("SproutBank", "Integrity: $it") }
        }

        return Report(cash, points, issues)
    }
}
