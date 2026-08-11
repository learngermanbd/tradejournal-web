package com.tradejournal.app.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TradeViewModel(private val repository: TradeRepository) : ViewModel() {
    private val _trades = MutableStateFlow<List<Trade>>(emptyList())
    val trades: StateFlow<List<Trade>> = _trades
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts
    private val _diaryNotes = MutableStateFlow<List<DiaryNote>>(emptyList())
    val diaryNotes: StateFlow<List<DiaryNote>> = _diaryNotes
    private val _importRecords = MutableStateFlow<List<ImportRecord>>(emptyList())
    val importRecords: StateFlow<List<ImportRecord>> = _importRecords
    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _trades.value = repository.loadTrades()
            _accounts.value = repository.loadAccounts()
            _diaryNotes.value = repository.loadDiaryNotes()
            _importRecords.value = repository.loadImportRecords()
        }
    }

    fun addTrade(trade: Trade) {
        viewModelScope.launch {
            repository.addTrade(trade)
            _trades.value = repository.loadTrades()
        }
    }

    fun seedIfEmpty(seed: List<Trade>) {
        viewModelScope.launch {
            repository.seedIfEmpty(seed)
            _trades.value = repository.loadTrades()
        }
    }

    fun seedAccountsIfEmpty(seed: List<Account>) {
        viewModelScope.launch {
            repository.seedAccountsIfEmpty(seed)
            _accounts.value = repository.loadAccounts()
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            repository.addAccount(account)
            _accounts.value = repository.loadAccounts()
        }
    }

    fun seedDiaryNotesIfEmpty(seed: List<DiaryNote>) {
        viewModelScope.launch {
            repository.seedDiaryNotesIfEmpty(seed)
            _diaryNotes.value = repository.loadDiaryNotes()
        }
    }

    fun addDiaryNote(note: DiaryNote) {
        viewModelScope.launch {
            repository.addDiaryNote(note)
            _diaryNotes.value = repository.loadDiaryNotes()
        }
    }

    fun importCsv(fileName: String, csv: String) {
        viewModelScope.launch {
            val summary = repository.importCsv(fileName, csv)
            _trades.value = repository.loadTrades()
            _importRecords.value = repository.loadImportRecords()
            _importStatus.value = "Imported ${summary.importedCount} trades; skipped ${summary.skippedCount} rows and ${summary.duplicateCount} duplicates."
        }
    }

    class Factory(private val repository: TradeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TradeViewModel::class.java))
            return TradeViewModel(repository) as T
        }
    }
}
