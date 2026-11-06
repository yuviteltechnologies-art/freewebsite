package com.sheetstore.admin.parser

import java.util.regex.Pattern

data class ParsedPayment(
    val utr: String,
    val amount: String,
    val isCredit: Boolean,
    val sender: String
)

object UpiParser {

    private val UTR_REGEX = Pattern.compile("(?i)(?:upi(?:/|:|-)?|ref(?:\\s*no)?[:.]?|utr[:.]?|rrn[:.]?)?\\s*(\\b\\d{12}\\b)")
    private val AMOUNT_REGEX = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposited", "added", "cr to", "credit")

    fun parseMessage(text: String, sender: String = ""): ParsedPayment? {
        val lower = text.lowercase()
        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        if (!isCredit) return null

        val utrMatcher = UTR_REGEX.matcher(text)
        var utr = ""
        while (utrMatcher.find()) {
            val candidate = utrMatcher.group(1)
            if (candidate != null && candidate.length == 12) {
                utr = candidate
                break
            }
        }

        if (utr.isEmpty()) {
            val fallbackMatcher = Pattern.compile("\\b\\d{12}\\b").matcher(text)
            if (fallbackMatcher.find()) {
                utr = fallbackMatcher.group()
            }
        }

        if (utr.isEmpty()) return null

        var amount = ""
        val amountMatcher = AMOUNT_REGEX.matcher(text)
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1)?.replace(",", "") ?: ""
        }

        return ParsedPayment(
            utr = utr,
            amount = amount,
            isCredit = true,
            sender = sender
        )
    }
}

