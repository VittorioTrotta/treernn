package com.example.expirationtracker.voice

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ParsedCommand(val productName: String, val expirationDate: LocalDate)

class VoiceCommandParser {

    private val monthMap = mapOf(
        "janeiro" to 1, "fevereiro" to 2, "março" to 3, "marco" to 3,
        "abril" to 4, "maio" to 5, "junho" to 6, "julho" to 7,
        "agosto" to 8, "setembro" to 9, "outubro" to 10,
        "novembro" to 11, "dezembro" to 12
    )

    // Matches: "DD de MONTH" or "DD de MONTH de YYYY"
    private val textDatePattern = Regex(
        """(\d{1,2})\s+de\s+(\w+)(?:\s+de\s+(\d{4}))?"""
    )

    // Matches: "DD/MM/YYYY" or "DD/MM"
    private val numericDatePattern = Regex(
        """(\d{1,2})/(\d{1,2})(?:/(\d{4}))?"""
    )

    // Trigger words that start a register command
    private val triggerWords = setOf("adicionar", "registrar", "cadastrar")

    // Keywords that mark the beginning of the date portion
    private val dateKeywords = listOf("expira em", "validade em", "vence em", "expira", "validade", "vence")

    fun parse(input: String): ParsedCommand? {
        if (input.isBlank()) return null

        val normalized = input.trim().lowercase()

        // Must start with a trigger word
        val trigger = triggerWords.firstOrNull { normalized.startsWith(it) } ?: return null

        val afterTrigger = normalized.removePrefix(trigger).trim()

        // Find the date keyword that splits product name from date
        val dateKeyword = dateKeywords.firstOrNull { afterTrigger.contains(it) } ?: return null

        val splitIndex = afterTrigger.indexOf(dateKeyword)
        val productName = afterTrigger.substring(0, splitIndex).trim()
        val datePart = afterTrigger.substring(splitIndex + dateKeyword.length).trim()

        if (productName.isEmpty()) return null

        val date = parseDate(datePart) ?: return null

        return ParsedCommand(productName = productName, expirationDate = date)
    }

    private fun parseDate(text: String): LocalDate? {
        // Try numeric format first: DD/MM/YYYY or DD/MM
        numericDatePattern.find(text)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: inferYear(month)
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        // Try text format: DD de MONTH [de YYYY]
        textDatePattern.find(text)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val monthName = match.groupValues[2].trim()
            val month = monthMap[monthName] ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: inferYear(month)
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        return null
    }

    // When year is omitted, use current year; advance to next year if date has already passed
    private fun inferYear(month: Int): Int {
        val today = LocalDate.now()
        val candidate = LocalDate.of(today.year, month, 1)
        return if (candidate.isBefore(today.withDayOfMonth(1))) today.year + 1 else today.year
    }
}
