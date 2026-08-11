package com.tradejournal.app.data

import java.util.UUID

data class DiaryNote(
    val id: String = UUID.randomUUID().toString(),
    val noteDate: String,
    val mood: String,
    val plan: String,
    val reflection: String,
    val createdAt: Long = System.currentTimeMillis(),
)
