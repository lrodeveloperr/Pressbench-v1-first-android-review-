package com.goodusestudios.pressbench.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionMetricsTest {
    @Test
    fun firstPassYieldSubtractsWasteAndRework() {
        val counts = ProductionCounts.normalized(processed = 100, waste = 2, rework = 10)

        assertEquals(88, counts.firstPassGood)
        assertEquals(0.88, counts.firstPassYield, 0.000001)
    }

    @Test
    fun recordPresentationCountsIgnoreStaleStoredGood() {
        val counts = record(processed = 100, storedGood = 98, waste = 2, rework = 10).productionCounts()

        assertEquals(88, counts.firstPassGood)
        assertEquals(0.88, counts.firstPassYield, 0.000001)
    }

    @Test
    fun invalidNegativeCountsNormalizeToZero() {
        val counts = ProductionCounts.normalized(processed = -5, waste = -2, rework = -8)

        assertEquals(0, counts.processed)
        assertEquals(0, counts.waste)
        assertEquals(0, counts.rework)
        assertEquals(0, counts.firstPassGood)
        assertEquals(0.0, counts.firstPassYield, 0.0)
    }

    @Test
    fun wasteAndReworkCanNeverExceedProcessed() {
        val counts = ProductionCounts.normalized(processed = 10, waste = 8, rework = 9)

        assertEquals(8, counts.waste)
        assertEquals(2, counts.rework)
        assertEquals(0, counts.firstPassGood)
    }

    @Test
    fun summaryRecalculatesFirstPassGoodFromSourceCounts() {
        val records = listOf(
            record(processed = 100, storedGood = 98, waste = 2, rework = 10),
            record(processed = 50, storedGood = 50, waste = 0, rework = 5),
        )

        val summary = records.productionSummary()

        assertEquals(150L, summary.processed)
        assertEquals(133L, summary.firstPassGood)
        assertEquals(2L, summary.waste)
        assertEquals(15L, summary.rework)
        assertEquals(133.0 / 150.0, summary.firstPassYield, 0.000001)
    }

    @Test
    fun summaryUsesLongTotalsWithoutIntegerOverflow() {
        val records = listOf(
            record(Int.MAX_VALUE, Int.MAX_VALUE, 0, 0),
            record(Int.MAX_VALUE, Int.MAX_VALUE, 0, 0),
        )

        val summary = records.productionSummary()

        assertEquals(Int.MAX_VALUE.toLong() * 2L, summary.processed)
        assertEquals(Int.MAX_VALUE.toLong() * 2L, summary.firstPassGood)
        assertEquals(1.0, summary.firstPassYield, 0.0)
    }

    private fun record(processed: Int, storedGood: Int, waste: Int, rework: Int) = RunRecord(
        id = "record-$processed-$waste-$rework",
        setupId = "setup",
        title = "Setup",
        timestamp = 0L,
        processed = processed,
        good = storedGood,
        waste = waste,
        rework = rework,
        issue = waste > 0 || rework > 0,
    )
}
