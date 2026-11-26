package com.sproutbank.app.ai

import com.sproutbank.app.data.Repository

/**
 * Lightweight, deterministic financial-literacy helper. It answers using
 * the current portfolio facts so children get context-aware explanations
 * without any network dependency.
 */
object SproutAssistant {

    fun ask(
        question: String,
        goalCashCents: Long,
        goalGrowthCents: Long,
        goalTargetCents: Long,
        etf: String,
    ): String {
        val q = question.lowercase()
        val boost = goalGrowthCents - goalCashCents
        return when {
            q.contains("spend") || q.contains("opportunity") ->
                "When you spend $${Repository.money(boost)} early, you don't just lose that " +
                "amount — you lose every future gain that money would have earned in $etf. " +
                "That's why the Spend Impact tab shows two lines."
            q.contains("etf") || q.contains("invest") ->
                "An ETF is a basket of many companies. $etf spreads your money so one " +
                "company doing badly doesn't sink your whole goal."
            q.contains("goal") || q.contains("save") ->
                "You've saved $${Repository.money(goalCashCents)} in cash and " +
                "$${Repository.money(goalGrowthCents)} with $etf growth. " +
                "Your target is $${Repository.money(goalTargetCents)}."
            q.contains("interest") || q.contains("compound") ->
                "Compounding means your gains earn gains. Every day you leave money " +
                "invested, it can grow a little more — that's why the green bar moves " +
                "faster than the blue one."
            q.contains("risk") || q.contains("lose") ->
                "ETFs can go down as well as up. That's why we show both the cash " +
                "line and the growth line — so you can see the range of outcomes."
            else ->
                "Great question! Try asking about spending, ETFs, goals, or compounding. " +
                "I can also explain what the Spend Impact chart shows."
        }
    }
}
