package com.app.biztrack.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val zone: ZoneId = ZoneId.systemDefault()
private val indonesianLocale: Locale = Locale.forLanguageTag("id-ID")
private val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", indonesianLocale)

fun todayMillis(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

fun parseDateMillis(value: String): Long? =
    try {
        LocalDate.parse(value, inputFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

fun formatDate(millis: Long, pattern: String = "yyyy-MM-dd"): String =
    DateTimeFormatter.ofPattern(pattern, indonesianLocale)
        .withZone(zone)
        .format(Instant.ofEpochMilli(millis))

fun monthStartMillis(month: YearMonth): Long =
    month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun monthEndMillis(month: YearMonth): Long =
    month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

fun currentMonth(): YearMonth = YearMonth.now(zone)

fun monthLabel(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", indonesianLocale))
