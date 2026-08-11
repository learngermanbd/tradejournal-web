package com.tradejournal.app.data

import java.util.UUID

data class ImportRecord(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val importedCount: Int,
    val skippedCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
