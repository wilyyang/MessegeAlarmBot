package com.messege.alarmbot.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. 기존 테이블 삭제
        db.execSQL("DROP TABLE IF EXISTS SanctionData")

        // 2. 새로운 테이블 생성
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS SanctionData (
                idx INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                eventAt INTEGER NOT NULL,
                giverId INTEGER NOT NULL,
                reason TEXT NOT NULL,
                sanctionCount INTEGER NOT NULL
            )
        """.trimIndent())
    }
}