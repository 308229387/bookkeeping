package com.sym.bookkeeping.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
private val percentFormat = NumberFormat.getPercentInstance(Locale.CHINA).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}

fun formatMoney(value: Double): String = currencyFormat.format(value)

/** 持股数未知（&lt;0）时显示「-」 */
fun formatShareCount(value: Int): String = if (value < 0) "-" else value.toString()

/** 税费未知（&lt;0）时显示「-」 */
fun formatFee(value: Double): String = if (value < 0) "-" else formatMoney(value)

fun formatSignedMoney(value: Double): String {
    val formatted = currencyFormat.format(kotlin.math.abs(value))
    return when {
        value > 0 -> "+$formatted"
        value < 0 -> "-$formatted"
        else -> formatted
    }
}

fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

fun formatPercent(ratio: Double): String = percentFormat.format(ratio)

/** 盈亏比按百分比数值存储，例如 -2.48 表示 -2.48% */
fun formatRatio(value: Double): String {
    val text = if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.2f".format(Locale.CHINA, value)
    }
    return if (value > 0) "+$text%" else "$text%"
}
