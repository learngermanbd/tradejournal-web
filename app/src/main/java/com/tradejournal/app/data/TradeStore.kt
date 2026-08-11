package com.tradejournal.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TradeStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private companion object {
        const val DATABASE_NAME = "tradejournal.db"
        const val DATABASE_VERSION = 2
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
        createSupportTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createSupportTables(db)
    }

    private fun createSupportTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                broker TEXT NOT NULL,
                balance REAL NOT NULL,
                equity REAL NOT NULL,
                currency TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS diary_notes (
                id TEXT PRIMARY KEY NOT NULL,
                noteDate TEXT NOT NULL,
                mood TEXT NOT NULL,
                plan TEXT NOT NULL,
                reflection TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
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

    fun readAccounts(): List<Account> = readableDatabase.query(
        "accounts",
        null,
        null,
        null,
        null,
        null,
        "name ASC",
    ).use { cursor ->
        buildList {
            val id = cursor.getColumnIndexOrThrow("id")
            val name = cursor.getColumnIndexOrThrow("name")
            val type = cursor.getColumnIndexOrThrow("type")
            val broker = cursor.getColumnIndexOrThrow("broker")
            val balance = cursor.getColumnIndexOrThrow("balance")
            val equity = cursor.getColumnIndexOrThrow("equity")
            val currency = cursor.getColumnIndexOrThrow("currency")
            while (cursor.moveToNext()) {
                add(Account(cursor.getString(id), cursor.getString(name), cursor.getString(type), cursor.getString(broker), cursor.getDouble(balance), cursor.getDouble(equity), cursor.getString(currency)))
            }
        }
    }

    fun insertAccount(account: Account) {
        val values = ContentValues().apply {
            put("id", account.id)
            put("name", account.name)
            put("type", account.type)
            put("broker", account.broker)
            put("balance", account.balance)
            put("equity", account.equity)
            put("currency", account.currency)
        }
        writableDatabase.insertWithOnConflict("accounts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun readDiaryNotes(): List<DiaryNote> = readableDatabase.query(
        "diary_notes",
        null,
        null,
        null,
        null,
        null,
        "createdAt DESC",
    ).use { cursor ->
        buildList {
            val id = cursor.getColumnIndexOrThrow("id")
            val noteDate = cursor.getColumnIndexOrThrow("noteDate")
            val mood = cursor.getColumnIndexOrThrow("mood")
            val plan = cursor.getColumnIndexOrThrow("plan")
            val reflection = cursor.getColumnIndexOrThrow("reflection")
            val createdAt = cursor.getColumnIndexOrThrow("createdAt")
            while (cursor.moveToNext()) {
                add(DiaryNote(cursor.getString(id), cursor.getString(noteDate), cursor.getString(mood), cursor.getString(plan), cursor.getString(reflection), cursor.getLong(createdAt)))
            }
        }
    }

    fun insertDiaryNote(note: DiaryNote) {
        val values = ContentValues().apply {
            put("id", note.id)
            put("noteDate", note.noteDate)
            put("mood", note.mood)
            put("plan", note.plan)
            put("reflection", note.reflection)
            put("createdAt", note.createdAt)
        }
        writableDatabase.insertWithOnConflict("diary_notes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
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
