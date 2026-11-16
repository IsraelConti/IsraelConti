package tec.mmhi.facturas

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

/**
 * Separate, persistent database for legally issued invoice numbers.
 * Never delete this database as part of clearing drafts or app cache.
 *
 * Integration requirement: create a backup/export of both invoice tables together.
 * Existing production databases must be migrated explicitly, not replaced.
 */
class IssuedInvoiceDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "mmhi_issued_invoices.db"
        const val DATABASE_VERSION = 1
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(AtomicInvoiceIssuer.CREATE_COUNTERS)
        db.execSQL(AtomicInvoiceIssuer.CREATE_ISSUED)
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_issued_invoices_date " +
                "ON issued_invoices(issued_on)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Fail closed: never silently drop legally issued invoices.
        throw IllegalStateException(
            "Migración de facturas emitidas pendiente: $oldVersion -> $newVersion"
        )
    }

    /**
     * Serializes calls sharing this helper instance; SQLite's write transaction
     * provides cross-connection locking. The caller must persist the final
     * invoice payload and never use this operation for draft numbers.
     */
    @Synchronized
    fun issue(series: String, date: LocalDate, payloadJson: String): String =
        AtomicInvoiceIssuer(writableDatabase).issue(series, date, payloadJson)

    fun findIssued(number: String): String? =
        readableDatabase.rawQuery(
            "SELECT payload_json FROM issued_invoices WHERE invoice_number=?",
            arrayOf(number)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
