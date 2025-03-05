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

val MIGRATION_3_4 = object : Migration(3,4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE MemberData ADD COLUMN likesWeekly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE MemberData ADD COLUMN dislikesWeekly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE MemberData ADD COLUMN giftPoints INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE MemberData ADD COLUMN resetPoints INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE MemberData ADD COLUMN rank TEXT NOT NULL DEFAULT 'Unemployed'")
    }
}