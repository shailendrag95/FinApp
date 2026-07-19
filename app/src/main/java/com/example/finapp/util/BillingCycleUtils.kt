package com.example.finapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun billingCycleRange(dueDateDay: Int?, referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long>? {
    if (dueDateDay == null || dueDateDay !in 1..28) return null
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(referenceMillis).atZone(zone).toLocalDate()
    val cycleEnd = resolveCycleEnd(today, dueDateDay)
    val cycleStart = cycleEnd.minusMonths(1).plusDays(1)
    return cycleStart.atStartOfDay(zone).toInstant().toEpochMilli() to
        cycleEnd.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
}

private fun resolveCycleEnd(today: LocalDate, dueDateDay: Int): LocalDate {
    val thisMonthEnd = today.withDayOfMonth(dueDateDay.coerceAtMost(today.lengthOfMonth()))
    return if (!today.isAfter(thisMonthEnd)) {
        thisMonthEnd
    } else {
        val nextMonth = today.plusMonths(1)
        nextMonth.withDayOfMonth(dueDateDay.coerceAtMost(nextMonth.lengthOfMonth()))
    }
}

fun currentMonthRange(referenceMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(referenceMillis).atZone(zone).toLocalDate()
    val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.withDayOfMonth(today.lengthOfMonth())
        .atTime(23, 59, 59)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
    return start to end
}
