package com.messege.alarmbot.core.common

enum class Rank(open val rank : Int, open val korName: String, open val tier: Int, open val resetPoints: Long, open val requirePoint: Long,
                open val isAdmin : Boolean = false, open val isSuperAdmin : Boolean  = false) {

    Villain(rank = -1, korName = "빌런", tier = -1, resetPoints = 0, requirePoint = -1),

    Unemployed(rank = 0, korName = "백수", tier = 0, resetPoints = 10, requirePoint = 0),

    JobSeeker(rank = 1, korName = "취준생", tier = 1, resetPoints = 10, requirePoint = 10),
    NewEmployee(rank = 2, korName = "신입", tier = 1, resetPoints = 10, requirePoint = 30),
    Staff(rank = 3, korName = "사원", tier = 1, resetPoints = 10, requirePoint = 80),

    Assistant(rank = 4, korName = "대리", tier = 2, resetPoints = 20, requirePoint = 160),
    Manager(rank = 5, korName = "과장", tier = 2, resetPoints = 20, requirePoint = 240),
    DeputyManager(rank = 6, korName = "차장", tier = 2, resetPoints = 20, requirePoint = 360),

    GeneralManager(rank = 7, korName = "부장", tier = 3, resetPoints = 30, requirePoint = 500),
    Director(rank = 8, korName = "이사", tier = 3, resetPoints = 30, requirePoint = 700),
    Boss(rank = 9, korName = "사장", tier = 3, resetPoints = 30, requirePoint = 900),

    Governor(rank = 10, korName = "도지사", tier = 4, resetPoints = 40, requirePoint = 1200),
    Congressman(rank = 11, korName = "국회의원", tier = 4, resetPoints = 45, requirePoint = 1500),

    SeoulMayor(rank = 12, korName = "서울시장", tier = 5, resetPoints = 50, requirePoint = 0),
    PrimeMinister(rank = 13, korName = "야당대표", tier = 5, resetPoints = 50, requirePoint = 0),

    Minister(rank = 14, korName = "부방장", tier = 6, resetPoints = 50, requirePoint = 0, isAdmin = true),
    President(rank = 15, korName = "방장", tier = 6, resetPoints = 50, requirePoint = 0, isAdmin = true, isSuperAdmin = true);

    companion object {
        fun getRankByPoint(point: Long): Rank {
            return if (point < 0) {
                Villain
            } else {
                entries
                    .filter { it.requirePoint >= 0 }
                    .sortedByDescending { it.requirePoint }
                    .firstOrNull { point >= it.requirePoint } ?: Unemployed
            }
        }

        fun getRankByName(paramName : String): Rank {
            return entries.firstOrNull { it.name == paramName } ?: Unemployed
        }
    }
}
