package com.goodusestudios.pressbench.i18n

import android.content.Context
import androidx.annotation.RawRes
import com.goodusestudios.pressbench.R
import org.json.JSONObject
import java.util.Locale

class PressBenchStrings private constructor(
    val supportedLocales: List<String>,
    private val catalog: Map<String, Map<String, String>>,
) {
    fun text(key: String, locale: String): String {
        val row = catalog[key]
        return row?.get(locale)
            ?: row?.get(locale.substringBefore('-'))
            ?: row?.get("en")
            ?: key.substringAfterLast('.').replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
                .replaceFirstChar { it.uppercase() }
    }

    fun displayLanguage(code: String, displayLocale: String): String = runCatching {
        val locale = localeFor(code)
        val display = localeFor(displayLocale)
        locale.getDisplayName(display).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.getOrDefault(code)

    fun isRtl(locale: String): Boolean = locale.substringBefore('-') in setOf("ar", "ur", "he")

    companion object {
        fun load(context: Context, @RawRes resource: Int = R.raw.pressbench_i18n): PressBenchStrings {
            val json = context.resources.openRawResource(resource).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val supportedJson = root.getJSONArray("supported")
            val supported = buildList {
                for (index in 0 until supportedJson.length()) add(supportedJson.getString(index))
            }
            val stringsJson = root.getJSONObject("strings")
            val strings = buildMap {
                stringsJson.keys().forEach { key ->
                    val rowJson = stringsJson.getJSONObject(key)
                    put(key, buildMap { rowJson.keys().forEach { code -> put(code, rowJson.getString(code)) } })
                }
            }
            return PressBenchStrings(supported, strings)
        }

        fun deviceLocale(supported: Collection<String>): String {
            val requested = Locale.getDefault().toLanguageTag()
            return normalizeLocale(requested, supported)
        }

        fun normalizeLocale(raw: String, supported: Collection<String>): String {
            val low = raw.replace('_', '-').lowercase(Locale.ROOT)
            if (low == "zh-hant" || low.startsWith("zh-tw") || low.startsWith("zh-hk") || low.startsWith("zh-mo")) {
                return if ("zh-Hant" in supported) "zh-Hant" else "zh"
            }
            if (low == "zh-hans" || low.startsWith("zh-cn") || low.startsWith("zh-sg")) return "zh"
            val aliases = mapOf("tl" to "fil", "no" to "nb", "iw" to "he", "in" to "id")
            val base = aliases[low.substringBefore('-')] ?: low.substringBefore('-')
            return base.takeIf { it in supported } ?: "en"
        }

        private fun localeFor(code: String): Locale = when (code) {
            "zh-Hant" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.forLanguageTag(code)
        }
    }
}
