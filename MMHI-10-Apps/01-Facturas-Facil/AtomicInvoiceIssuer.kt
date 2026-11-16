package tec.mmhi.facturas

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

/**
 * SQLite-backed, atomic invoice numbering. Call from a single SQLiteOpenHelper-managed
 * database. Both the sequence reservation and the issued invoice insertion occur in
 * one transaction; failures roll back both. Do not use for draft numbering.
 *
 * This module must be integrated into the Android app before it is considered shipped.
 */
class AtomicInvoiceIssuer(private val database: SQLiteDatabase) {
    companion object {
        const val CREATE_COUNTERS = """
            CREATE TABLE IF NOT EXISTS invoice_counters (
                series TEXT NOT NULL,
                year INTEGER NOT NULL,
                last_sequence INTEGER NOT NULL CHECK(last_sequence BETWEEN 1 AND 999999999),
                PRIMARY KEY(series, year)
            )
        """
        const val CREATE_ISSUED = """
            CREATE TABLE IF NOT EXISTS issued_invoices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_number TEXT NOT NULL UNIQUE,
                series TEXT NOT NULL,
                year INTEGER NOT NULL,
                sequence INTEGER NOT NULL,
                issued_on TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                UNIQUE(series, year, sequence)
            )
        """
    }

    /** Run schema migrations once, during SQLiteOpenHelper.onCreate/onUpgrade. */
    fun issue(seriesInput: String, date: LocalDate, payloadJson: String): String {
        val series = InvoiceNumbering.validateSeries(seriesInput)
        require(payloadJson.isNotBlank()) { "Factura sin contenido" }
        require(date.year in 2000..9999) { "Año inválido" }
        database.beginTransaction()
        try {
            val previous = database.rawQuery(
                "SELECT last_sequence FROM invoice_counters WHERE series=? AND year=?",
                arrayOf(series, date.year.toString())
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            check(previous < 999999999L) { "Serie agotada" }
            val sequence = previous + 1L
            val counter = ContentValues().apply {
                put("series", series)
                put("year", date.year)
                put("last_sequence", sequence)
            }
            database.insertWithOnConflict(
                "invoice_counters", null, counter, SQLiteDatabase.CONFLICT_REPLACE
            ).also { check(it != -1L) { "No se pudo reservar el número" } }

            val number = InvoiceNumbering.format(series, date.year, sequence)
            val invoice = ContentValues().apply {
                put("invoice_number", number)
                put("series", series)
                put("year", date.year)
                put("sequence", sequence)
                put("issued_on", date.toString())
                put("payload_json", payloadJson)
            }
            database.insertOrThrow("issued_invoices", null, invoice)
            database.setTransactionSuccessful()
            return number
        } finally {
            database.endTransaction()
        }
    }
}
