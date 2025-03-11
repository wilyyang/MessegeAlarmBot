package com.messege.alarmbot.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.messege.alarmbot.core.common.Rank

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
        db.execSQL("ALTER TABLE MemberData ADD COLUMN rank TEXT NOT NULL DEFAULT '${Rank.Unemployed.name}'")
    }
}

val MIGRATION_4_5 = object : Migration(4,5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. 기존 테이블 이름을 변경 (백업)
        db.execSQL("ALTER TABLE MemberData RENAME TO MemberData_old")

        // 2. 새로운 테이블 생성 (새로운 컬럼 추가됨)
        db.execSQL("""
            CREATE TABLE MemberData (
                idx INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                createAt INTEGER NOT NULL,
                profileType INTEGER NOT NULL,
                latestName TEXT NOT NULL,
                isSuperAdmin INTEGER NOT NULL,
                isAdmin INTEGER NOT NULL,
                chatProfileCount INTEGER NOT NULL,
                talkCount INTEGER NOT NULL,
                deleteTalkCount INTEGER NOT NULL,
                enterCount INTEGER NOT NULL,
                kickCount INTEGER NOT NULL,
                sanctionCount INTEGER NOT NULL,
                likes INTEGER NOT NULL,
                dislikes INTEGER NOT NULL,
                likesWeekly INTEGER NOT NULL,
                dislikesWeekly INTEGER NOT NULL,
                giftPoints INTEGER NOT NULL,
                resetPoints INTEGER NOT NULL,
                rank TEXT NOT NULL,
                partyId INTEGER NOT NULL,
                partyState TEXT NOT NULL DEFAULT 'None',
                joinTime INTEGER NOT NULL DEFAULT -1,
                partyResetPoints INTEGER NOT NULL DEFAULT 0
            )
        """)

        // 3. 기존 데이터 이전 (새로운 컬럼은 초기값 적용)
        db.execSQL("""
            INSERT INTO MemberData (
                idx, userId, createAt, profileType, latestName, isSuperAdmin, isAdmin, chatProfileCount,
                talkCount, deleteTalkCount, enterCount, kickCount, sanctionCount, likes, dislikes,
                likesWeekly, dislikesWeekly, giftPoints, resetPoints, rank, partyId,
                partyState, joinTime, partyResetPoints
            )
            SELECT 
                idx, userId, createAt, profileType, latestName, isSuperAdmin, isAdmin, chatProfileCount,
                talkCount, deleteTalkCount, enterCount, kickCount, sanctionCount, likes, dislikes,
                likesWeekly, dislikesWeekly, giftPoints, resetPoints, rank, partyId,
                'None', -1, 0
            FROM MemberData_old
        """)

        // 4. 기존 테이블 삭제
        db.execSQL("DROP TABLE MemberData_old")

        // 5. 새로운 PartyData 테이블 추가
        db.execSQL("""
            CREATE TABLE PartyData (
                idx INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                foundingTime INTEGER NOT NULL,
                name TEXT NOT NULL,
                partyPoints INTEGER NOT NULL,
                leaderId INTEGER NOT NULL,
                partyState TEXT NOT NULL DEFAULT 'None',
                memberCount INTEGER NOT NULL DEFAULT 0,
                description TEXT NOT NULL DEFAULT ''
            )
        """)

        // 6. 새로운 PartyRule 테이블 추가
        db.execSQL("""
            CREATE TABLE PartyRule (
                idx INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                time INTEGER NOT NULL,
                partyId INTEGER NOT NULL,
                memberId INTEGER NOT NULL,
                rule TEXT NOT NULL
            )
        """)

        // 7. 새로운 PartyLog 테이블 추가
        db.execSQL("""
            CREATE TABLE PartyLog (
                idx INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                time INTEGER NOT NULL,
                partyId INTEGER NOT NULL,
                memberId INTEGER NOT NULL,
                logType TEXT NOT NULL
            )
        """)
    }
}