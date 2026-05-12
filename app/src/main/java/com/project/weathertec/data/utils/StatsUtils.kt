package com.project.weathertec.data.utils

import com.project.weathertec.data.model.WeatherRecord

/**
 * Equivalente en Kotlin de statsUtils.js de la web.
 * Todas las funciones de cálculo y agrupación de datos.
 */
object StatsUtils {

    // ── Extracción de valores ──────────────────────────────────────────────

    fun getValues(records: List<WeatherRecord>, key: String): List<Double> {
        return records.mapNotNull { r ->
            when (key) {
                "temperature" -> r.temperature
                "humidity"    -> r.humidity
                "windSpeed"   -> r.windSpeed
                else          -> null
            }
        }.filter { !it.isNaN() }
    }

    // ── Estadísticas básicas ───────────────────────────────────────────────

    data class Stats(
        val avg: Double,
        val min: Double,
        val max: Double,
        val median: Double,
        val count: Int
    )

    fun calcStats(values: List<Double>): Stats? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        return Stats(
            avg    = values.average(),
            min    = sorted.first(),
            max    = sorted.last(),
            median = sorted[sorted.size / 2],
            count  = values.size
        )
    }

    // ── Agrupación por hora ────────────────────────────────────────────────

    data class HourGroup(
        val hour: String,     // "08:00"
        val hourNum: Int,     // 8
        val stats: Stats?,
        val values: List<Double>
    )

    fun groupByHour(records: List<WeatherRecord>, key: String): List<HourGroup> {
        val groups = mutableMapOf<String, MutableList<Double>>()

        for (r in records) {
            val hour = r.time.take(2).padStart(2, '0')
            val value = when (key) {
                "temperature" -> r.temperature
                "humidity"    -> r.humidity
                "windSpeed"   -> r.windSpeed
                else          -> null
            } ?: continue
            groups.getOrPut(hour) { mutableListOf() }.add(value)
        }

        return groups.entries
            .map { (hour, vals) ->
                HourGroup(
                    hour    = "$hour:00",
                    hourNum = hour.toIntOrNull() ?: 0,
                    stats   = calcStats(vals),
                    values  = vals
                )
            }
            .sortedBy { it.hourNum }
    }

    // ── Agrupación por día ─────────────────────────────────────────────────

    data class DayGroup(
        val date: String,
        val stats: Stats?,
        val values: List<Double>
    )

    fun groupByDay(records: List<WeatherRecord>, key: String): List<DayGroup> {
        val groups = mutableMapOf<String, MutableList<Double>>()

        for (r in records) {
            val value = when (key) {
                "temperature" -> r.temperature
                "humidity"    -> r.humidity
                "windSpeed"   -> r.windSpeed
                else          -> null
            } ?: continue
            groups.getOrPut(r.date) { mutableListOf() }.add(value)
        }

        return groups.entries
            .map { (date, vals) ->
                DayGroup(
                    date   = date,
                    stats  = calcStats(vals),
                    values = vals
                )
            }
            .sortedBy { it.date }
    }

    // ── Extremos (máximo y mínimo con su registro completo) ─────────────────

    data class Extremes(
        val maxRecord: WeatherRecord?,
        val minRecord: WeatherRecord?
    )

    fun findExtremes(records: List<WeatherRecord>, key: String): Extremes {
        var maxRecord: WeatherRecord? = null
        var minRecord: WeatherRecord? = null

        for (r in records) {
            val value = when (key) {
                "temperature" -> r.temperature
                "humidity"    -> r.humidity
                "windSpeed"   -> r.windSpeed
                else          -> null
            } ?: continue

            val maxVal = when (key) {
                "temperature" -> maxRecord?.temperature
                "humidity"    -> maxRecord?.humidity
                "windSpeed"   -> maxRecord?.windSpeed
                else          -> null
            }
            val minVal = when (key) {
                "temperature" -> minRecord?.temperature
                "humidity"    -> minRecord?.humidity
                "windSpeed"   -> minRecord?.windSpeed
                else          -> null
            }

            if (maxVal == null || value > maxVal) maxRecord = r
            if (minVal == null || value < minVal) minRecord = r
        }

        return Extremes(maxRecord, minRecord)
    }

    // ── Filtrado de outliers (IQR) ─────────────────────────────────────────

    data class OutlierResult(
        val filtered: List<Double>,
        val removed: List<Double>,
        val lower: Double,
        val upper: Double,
        val q1: Double,
        val q3: Double,
        val iqr: Double
    )

    fun filterOutliers(values: List<Double>, factor: Double = 1.5): OutlierResult? {
        if (values.size < 4) return null
        val sorted = values.sorted()
        val q1 = sorted[(sorted.size * 0.25).toInt()]
        val q3 = sorted[(sorted.size * 0.75).toInt()]
        val iqr = q3 - q1
        val lower = q1 - factor * iqr
        val upper = q3 + factor * iqr

        val filtered = mutableListOf<Double>()
        val removed  = mutableListOf<Double>()
        for (v in values) {
            if (v in lower..upper) filtered.add(v) else removed.add(v)
        }
        return OutlierResult(filtered, removed, lower, upper, q1, q3, iqr)
    }

    fun filterOutlierRecords(
        records: List<WeatherRecord>,
        key: String,
        factor: Double = 1.5
    ): List<WeatherRecord> {
        val values = getValues(records, key)
        val result = filterOutliers(values, factor) ?: return records
        return records.filter { r ->
            val v = when (key) {
                "temperature" -> r.temperature
                "humidity"    -> r.humidity
                "windSpeed"   -> r.windSpeed
                else          -> null
            } ?: return@filter true
            v in result.lower..result.upper
        }
    }

    // ── Formato para display ───────────────────────────────────────────────

    fun formatValue(value: Double?, decimals: Int = 1): String {
        if (value == null || value.isNaN()) return "—"
        return "%.${decimals}f".format(value)
    }

    fun formatDate(dateStr: String): String {
        if (dateStr.length < 10) return dateStr
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        val months = listOf("Ene","Feb","Mar","Abr","May","Jun",
                            "Jul","Ago","Sep","Oct","Nov","Dic")
        val month = parts[1].toIntOrNull()?.minus(1) ?: return dateStr
        return "${parts[2].trimStart('0')} ${months.getOrElse(month) { "?" }} ${parts[0]}"
    }
}
