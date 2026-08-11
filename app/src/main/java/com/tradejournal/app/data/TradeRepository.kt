package com.tradejournal.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TradeRepository(private val store: TradeStore) {
    suspend fun loadTrades(): List<Trade> = withContext(Dispatchers.IO) { store.readAll() }

    suspend fun addTrade(trade: Trade) = withContext(Dispatchers.IO) { store.insert(trade) }

    suspend fun importCsv(fileName: String, csv: String): ImportSummary = withContext(Dispatchers.IO) {
        val parsed = CsvTradeImporter.parse(csv)
        val existingFingerprints = store.readTradeFingerprints().toMutableSet()
        var duplicates = 0
        val newTrades = parsed.trades.filter { trade ->
            if (trade.sourceFingerprint.isNotBlank() && !existingFingerprints.add(trade.sourceFingerprint)) {
                duplicates++
                false
            } else true
        }
        newTrades.forEach(store::insert)
        store.insertImportRecord(ImportRecord(fileName = fileName, importedCount = newTrades.size, skippedCount = parsed.skippedRows + duplicates))
        ImportSummary(newTrades.size, parsed.skippedRows, duplicates, parsed.errors)
    }

    suspend fun loadImportRecords(): List<ImportRecord> = withContext(Dispatchers.IO) { store.readImportRecords() }

    suspend fun seedIfEmpty(seed: List<Trade>) = withContext(Dispatchers.IO) {
        if (store.count() == 0) seed.forEach(store::insert)
    }

    suspend fun loadAccounts(): List<Account> = withContext(Dispatchers.IO) { store.readAccounts() }

    suspend fun seedAccountsIfEmpty(seed: List<Account>) = withContext(Dispatchers.IO) {
        if (store.readAccounts().isEmpty()) seed.forEach(store::insertAccount)
    }

    suspend fun loadDiaryNotes(): List<DiaryNote> = withContext(Dispatchers.IO) { store.readDiaryNotes() }

    suspend fun seedDiaryNotesIfEmpty(seed: List<DiaryNote>) = withContext(Dispatchers.IO) {
        if (store.readDiaryNotes().isEmpty()) seed.forEach(store::insertDiaryNote)
    }

    suspend fun addDiaryNote(note: DiaryNote) = withContext(Dispatchers.IO) { store.insertDiaryNote(note) }
}

data class ImportSummary(
    val importedCount: Int,
    val skippedCount: Int,
    val duplicateCount: Int,
    val errors: List<String>,
)
