package com.goodusestudios.pressbench.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.goodusestudios.pressbench.i18n.PressBenchStrings
import com.goodusestudios.pressbench.model.RunRecord
import com.goodusestudios.pressbench.model.productionCounts
import com.goodusestudios.pressbench.model.productionSummary
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object ReportExporter {
    fun shareCsv(context: Context, history: List<RunRecord>, localeCode: String, strings: PressBenchStrings) {
        val file = File(context.cacheDir, "pressbench-production-report.csv")
        val locale = Locale.forLanguageTag(localeCode)
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine(
                listOf("runs.title", "runState.completed", "report.unitsProcessed", "report.firstPassYield", "report.wasteUnits", "report.reworkedUnits", "result.addIssue", "common.notes")
                    .joinToString(",") { csv(strings.text(it, localeCode)) },
            )
            history.forEach { row ->
                val counts = row.productionCounts()
                val firstPassYield = "${(counts.firstPassYield * 100.0).roundToInt()}%"
                writer.appendLine(
                    listOf(row.titleKey?.let { strings.text(it, localeCode) } ?: row.title, formatter.format(Date(row.timestamp)), counts.processed, firstPassYield, counts.waste, counts.rework, row.issue, row.note)
                        .joinToString(",") { csv(it.toString()) },
                )
            }
        }
        share(context, file, "text/csv")
    }

    fun sharePdf(context: Context, history: List<RunRecord>, localeCode: String, strings: PressBenchStrings) {
        val file = File(context.cacheDir, "pressbench-production-report.pdf")
        val document = PdfDocument()
        val width = 612
        val height = 792
        val margin = 42f
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 23f; typeface = Typeface.DEFAULT_BOLD }
        val subhead = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = 0xFF53666F.toInt() }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9E5E9.toInt(); strokeWidth = 1f }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun drawHeader() {
            canvas.drawText("PressBench", margin, y, header); y += 25f
            canvas.drawText(strings.text("report.productionReport", localeCode), margin, y, subhead); y += 18f
            val summary = history.productionSummary()
            val yield = (summary.firstPassYield * 100.0).roundToInt()
            canvas.drawText(
                "${strings.text("report.unitsProcessed", localeCode)} ${summary.processed}     ${strings.text("report.firstPassYield", localeCode)} $yield%     ${strings.text("report.wasteUnits", localeCode)} ${summary.waste}",
                margin, y, body,
            ); y += 22f
            canvas.drawLine(margin, y, width - margin, y, line); y += 18f
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page.canvas
            y = margin
            drawHeader()
        }

        drawHeader()
        val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag(localeCode))
        history.forEach { row ->
            val counts = row.productionCounts()
            if (y > height - 70) newPage()
            canvas.drawText((row.titleKey?.let { strings.text(it, localeCode) } ?: row.title).take(58), margin, y, subhead)
            canvas.drawText("${counts.firstPassGood}/${counts.processed}", width - margin - 42f, y, subhead)
            y += 14f
            canvas.drawText(formatter.format(Date(row.timestamp)), margin, y, muted)
            canvas.drawText("${strings.text("report.wasteUnits", localeCode)} ${counts.waste}  ·  ${strings.text("report.reworkedUnits", localeCode)} ${counts.rework}", margin + 235f, y, muted)
            y += 16f
            canvas.drawLine(margin, y, width - margin, y, line)
            y += 15f
        }
        document.finishPage(page)
        FileOutputStream(file).use(document::writeTo)
        document.close()
        share(context, file, "application/pdf")
    }

    private fun share(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PressBench"))
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
