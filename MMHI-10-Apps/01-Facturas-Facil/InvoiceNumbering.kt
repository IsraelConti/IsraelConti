package tec.mmhi.facturas

import java.time.LocalDate

/** Pure Kotlin/JVM invoice numbering. Drafts never consume an issued invoice number. */
object InvoiceNumbering {
    private val seriesPattern = Regex("[A-Z0-9][A-Z0-9_-]{0,15}")

    fun validateSeries(series: String): String {
        val normalized = series.trim().uppercase(java.util.Locale.ROOT)
        require(seriesPattern.matches(normalized)) { "Serie inválida" }
        return normalized
    }

    fun format(series: String, year: Int, sequence: Long): String {
        require(year in 2000..9999) { "Año inválido" }
        require(sequence in 1..999999999L) { "Secuencia fuera de rango" }
        return "${validateSeries(series)}-$year-${sequence.toString().padStart(6, '0')}"
    }

    fun nextSequence(issuedNumbers: Iterable<String>, series: String, year: Int): Long {
        val prefix = "${validateSeries(series)}-$year-"
        val highest = issuedNumbers.asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).takeIf { suffix -> suffix.isNotEmpty() && suffix.all(Char::isDigit) }?.toLongOrNull() }
            .filter { it in 1..999999999L }
            .maxOrNull() ?: 0L
        check(highest < 999999999L) { "Serie agotada" }
        return highest + 1
    }

    fun nextNumber(issuedNumbers: Iterable<String>, series: String, date: LocalDate): String =
        format(series, date.year, nextSequence(issuedNumbers, series, date.year))
}
