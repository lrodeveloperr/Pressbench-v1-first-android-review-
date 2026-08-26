package com.goodusestudios.pressbench.model

data class ProductionCounts private constructor(
    val processed: Int,
    val waste: Int,
    val rework: Int,
) {
    val firstPassGood: Int = processed - waste - rework
    val firstPassYield: Double = if (processed == 0) 0.0 else firstPassGood.toDouble() / processed

    companion object {
        fun normalized(processed: Int, waste: Int, rework: Int): ProductionCounts {
            val safeProcessed = processed.coerceAtLeast(0)
            val safeWaste = waste.coerceIn(0, safeProcessed)
            val safeRework = rework.coerceIn(0, safeProcessed - safeWaste)
            return ProductionCounts(safeProcessed, safeWaste, safeRework)
        }
    }
}

data class ProductionSummary(
    val processed: Long,
    val firstPassGood: Long,
    val waste: Long,
    val rework: Long,
) {
    val firstPassYield: Double = if (processed == 0L) 0.0 else firstPassGood.toDouble() / processed
}

fun Iterable<RunRecord>.productionSummary(): ProductionSummary {
    var processed = 0L
    var firstPassGood = 0L
    var waste = 0L
    var rework = 0L
    for (record in this) {
        val counts = record.productionCounts()
        processed += counts.processed.toLong()
        firstPassGood += counts.firstPassGood.toLong()
        waste += counts.waste.toLong()
        rework += counts.rework.toLong()
    }
    return ProductionSummary(processed, firstPassGood, waste, rework)
}

fun RunRecord.productionCounts(): ProductionCounts =
    ProductionCounts.normalized(processed, waste, rework)
