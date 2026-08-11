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

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _trades.value = repository.loadTrades() }
    }

    fun addTrade(trade: Trade) {
        viewModelScope.launch {
            repository.addTrade(trade)
            refresh()
        }
    }

    fun seedIfEmpty(seed: List<Trade>) {
        viewModelScope.launch {
            repository.seedIfEmpty(seed)
            refresh()
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
