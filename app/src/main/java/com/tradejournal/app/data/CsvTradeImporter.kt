package com.tradejournal.app.data

import java.security.MessageDigest

object CsvTradeImporter {
    data class Result(
        val trades: List<Trade>,
        val skippedRows: Int,
        val errors: List<String>,
    )

    fun parse(csv: String): Result {
        val rows = csv.lineSequence().filter { it.isNotBlank() }.map(::parseLine).toList()
        if (rows.isEmpty()) return Result(emptyList(), 0, listOf("The CSV file is empty."))

        val headers = rows.first().map { normalize(it) }
        val symbolIndex = findIndex(headers, "symbol", "ticker", "instrument")
        val resultIndex = findIndex(headers, "pnl", "profit", "profitloss", "realizedpnl", "result")
        if (symbolIndex == -1 || resultIndex == -1) {
            return Result(emptyList(), rows.drop(1).size, listOf("Required columns: symbol/ticker and pnl/profit/result."))
        }

        val marketIndex = findIndex(headers, "market", "assettype", "asset")
        val directionIndex = findIndex(headers, "direction", "side", "position")
        val setupIndex = findIndex(headers, "setup", "strategy", "tag")
        val riskIndex = findIndex(headers, "r", "rmultiple", "riskmultiple")
        val trades = mutableListOf<Trade>()
        val errors = mutableListOf<String>()
        var skipped = 0

        rows.drop(1).forEachIndexed { offset, row ->
            val rowNumber = offset + 2
            val symbol = row.valueAt(symbolIndex).trim()
            val result = row.valueAt(resultIndex).replace(",", "").trim().toDoubleOrNull()
            if (symbol.isBlank() || result == null) {
                skipped++
                errors += "Row $rowNumber skipped: symbol or numeric P&L is missing."
            } else {
                val direction = row.valueAt(directionIndex).ifBlank { "Unknown" }.replaceFirstChar { it.uppercase() }
                val setup = row.valueAt(setupIndex).ifBlank { "Imported" }
                val rMultiple = row.valueAt(riskIndex).toDoubleOrNull() ?: 0.0
                val fingerprint = fingerprint(row.joinToString("|") { it.trim() })
                trades += Trade(
                    symbol = symbol.uppercase(),
                    market = row.valueAt(marketIndex).ifBlank { "Imported" },
                    direction = direction,
                    setup = setup,
                    result = result,
                    rMultiple = rMultiple,
                    status = "Imported",
                    sourceFingerprint = fingerprint,
                )
            }
        }
        return Result(trades, skipped, errors)
    }

    private fun findIndex(headers: List<String>, vararg candidates: String): Int =
        headers.indexOfFirst { header -> candidates.any { header == it } }

    private fun normalize(value: String): String = value.trim().lowercase().replace(" ", "").replace("_", "").replace("-", "")

    private fun List<String>.valueAt(index: Int): String = if (index in indices) this[index] else ""

    private fun parseLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            when (val character = line[index]) {
                '"' -> if (quoted && index + 1 < line.length && line[index + 1] == '"') { current.append('"'); index++ } else quoted = !quoted
                ',' -> if (quoted) current.append(character) else { values += current.toString(); current.clear() }
                else -> current.append(character)
            }
            index++
        }
        values += current.toString()
        return values
    }

    private fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
