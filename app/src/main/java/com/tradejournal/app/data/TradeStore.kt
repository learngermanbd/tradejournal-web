package com.tradejournal.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TradeStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private companion object {
        const val DATABASE_NAME = "tradejournal.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE trades (
                id TEXT PRIMARY KEY NOT NULL,
                symbol TEXT NOT NULL,
                market TEXT NOT NULL,
                direction TEXT NOT NULL,
                setup TEXT NOT NULL,
                result REAL NOT NULL,
                rMultiple REAL NOT NULL,
                status TEXT NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema migrations will be added here before the first production release.
    }

    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM trades", null).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    fun readAll(): List<Trade> = readableDatabase.query(
        "trades",
        null,
        null,
        null,
        null,
        null,
        "createdAt DESC",
    ).use { cursor ->
        buildList {
            val id = cursor.getColumnIndexOrThrow("id")
            val symbol = cursor.getColumnIndexOrThrow("symbol")
            val market = cursor.getColumnIndexOrThrow("market")
            val direction = cursor.getColumnIndexOrThrow("direction")
            val setup = cursor.getColumnIndexOrThrow("setup")
            val result = cursor.getColumnIndexOrThrow("result")
            val rMultiple = cursor.getColumnIndexOrThrow("rMultiple")
            val status = cursor.getColumnIndexOrThrow("status")
            val note = cursor.getColumnIndexOrThrow("note")
            val createdAt = cursor.getColumnIndexOrThrow("createdAt")
            while (cursor.moveToNext()) {
                add(
                    Trade(
                        id = cursor.getString(id),
                        symbol = cursor.getString(symbol),
                        market = cursor.getString(market),
                        direction = cursor.getString(direction),
                        setup = cursor.getString(setup),
                        result = cursor.getDouble(result),
                        rMultiple = cursor.getDouble(rMultiple),
                        status = cursor.getString(status),
                        note = cursor.getString(note),
                        createdAt = cursor.getLong(createdAt),
                    ),
                )
            }
        }
    }

    fun insert(trade: Trade) {
        val values = ContentValues().apply {
            put("id", trade.id)
            put("symbol", trade.symbol)
            put("market", trade.market)
            put("direction", trade.direction)
            put("setup", trade.setup)
            put("result", trade.result)
            put("rMultiple", trade.rMultiple)
            put("status", trade.status)
            put("note", trade.note)
            put("createdAt", trade.createdAt)
        }
        writableDatabase.insertWithOnConflict("trades", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
