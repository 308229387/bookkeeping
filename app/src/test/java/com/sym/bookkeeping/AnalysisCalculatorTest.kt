package com.sym.bookkeeping

import com.sym.bookkeeping.data.AnalysisCalculator
import com.sym.bookkeeping.data.TradeRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisCalculatorTest {
    @Test
    fun winRateGroupedByMethod() {
        val records = listOf(
            TradeRecord(method = "突破", profit = 100.0),
            TradeRecord(method = "突破", profit = -50.0),
            TradeRecord(method = "突破", profit = 80.0),
            TradeRecord(method = "回踩", profit = -20.0),
            TradeRecord(method = "回踩", profit = -10.0)
        )

        val byMethod = AnalysisCalculator.byMethod(records)
        val breakout = byMethod.first { it.method == "突破" }
        val pullback = byMethod.first { it.method == "回踩" }

        assertEquals(3, breakout.totalCount)
        assertEquals(2, breakout.winCount)
        assertEquals(2.0 / 3.0, breakout.winRate, 1e-6)

        assertEquals(2, pullback.totalCount)
        assertEquals(0, pullback.winCount)
        assertEquals(0.0, pullback.winRate, 1e-6)

        val overall = AnalysisCalculator.overall(records)
        assertEquals(5, overall.totalCount)
        assertEquals(2, overall.winCount)
        assertEquals(100.0, overall.totalProfit, 1e-6)
    }
}
