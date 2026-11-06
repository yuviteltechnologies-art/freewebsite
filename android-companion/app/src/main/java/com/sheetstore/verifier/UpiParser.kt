package com.sheetstore.verifier

import java.util.regex.Pattern

data class ParsedPayment(
    val utr: String,
    val amount: String,
    val isCredit: Boolean,
    val sender: String
)

object UpiParser {

    // Regex to match 12-digit UTR numbers:
    // e.g. "UPI/412984920194", "Ref no 412984920194", "UTR: 412984920194"
    private val UTR_REGEX = Pattern.compile("(?i)(?:upi(?:/|:|-)?|ref(?:\\s*no)?[:.]?|utr[:.]?|rrn[:.]?)?\\s*(\\b\\d{12}\\b)")

    // Regex to match Indian Rupee amounts:
    // e.g. "Rs. 499.00", "INR 499", "Rs 499", "credited with Rs 499"
    private val AMOUNT_REGEX = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")

    // Keywords indicating money came into your account
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposited", "added", "cr to", "credit")

    fun parseMessage(text: String, sender: String = ""): ParsedPayment? {
        val lower = text.lowercase()

        // 1. Check if message is a CREDIT (money received, not spent)
        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        if (!isCredit) return null

        // 2. Extract UTR
        val utrMatcher = UTR_REGEX.matcher(text)
        var utr = ""
        while (utrMatcher.find()) {
            val candidate = utrMatcher.group(1)
            if (candidate != null && candidate.length == 12) {
                utr = candidate
                break
            }
        }

        // Fallback: search for any standalone 12-digit number
        if (utr.isEmpty()) {
            val fallbackMatcher = Pattern.compile("\\b\\d{12}\\b").matcher(text)
            if (fallbackMatcher.find()) {
                utr = fallbackMatcher.group()
            }
        }

        if (utr.isEmpty()) return null

        // 3. Extract Amount
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

