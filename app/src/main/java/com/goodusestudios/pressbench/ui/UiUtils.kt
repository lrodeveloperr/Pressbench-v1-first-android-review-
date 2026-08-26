package com.goodusestudios.pressbench.ui

import com.goodusestudios.pressbench.model.Pressure
import com.goodusestudios.pressbench.model.Setup
import com.goodusestudios.pressbench.model.TemperatureUnit
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

typealias Translator = (String) -> String

fun localeFor(code: String): Locale = Locale.forLanguageTag(code)

fun localTitle(setup: Setup, t: Translator): String = setup.titleKey?.let(t) ?: setup.title
fun localMaterial(setup: Setup, t: Translator): String = setup.materialKey?.let(t) ?: setup.material
fun localSource(setup: Setup, t: Translator): String = setup.sourceKey?.let(t) ?: setup.source

fun formatNumber(value: Number, locale: String): String = NumberFormat.getIntegerInstance(localeFor(locale)).format(value)

fun formatPercent(decimal: Number, locale: String): String =
    NumberFormat.getPercentInstance(localeFor(locale)).apply { maximumFractionDigits = 0 }.format(decimal.toDouble())

fun formatDate(timestamp: Long, locale: String): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, localeFor(locale)).format(Date(timestamp))

fun formatShortDate(timestamp: Long, locale: String): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, localeFor(locale)).format(Date(timestamp))

fun pressureText(pressure: Pressure, t: Translator): String = when (pressure) {
    Pressure.LIGHT -> t("pressure.light")
    Pressure.MEDIUM -> t("pressure.medium")
    Pressure.FIRM -> t("pressure.firm")
}

fun formatTemperature(raw: String, unit: TemperatureUnit, locale: String): String {
    val match = Regex("-?\\d+(?:[.,]\\d+)?").find(raw) ?: return raw
    var value = match.value.replace(',', '.').toDoubleOrNull() ?: return raw
    val source = if (raw.contains('C', ignoreCase = true)) TemperatureUnit.C else TemperatureUnit.F
    val target = if (unit == TemperatureUnit.AUTO) source else unit
    if (source != target) value = if (target == TemperatureUnit.C) (value - 32) * 5 / 9 else value * 9 / 5 + 32
    return "${NumberFormat.getIntegerInstance(localeFor(locale)).format(value.roundToInt())}°${target.name}"
}
