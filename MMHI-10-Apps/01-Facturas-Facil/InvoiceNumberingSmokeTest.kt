package tec.mmhi.facturas

import java.time.LocalDate

/** Run on JVM with kotlinc; tests numbering logic only, not Android or database locking. */
object InvoiceNumberingSmokeTest {
    @JvmStatic fun main(args: Array<String>) {
        var count = 0
        fun verify(value: Boolean) { check(value); count++ }
        fun rejects(action: () -> Unit) {
            try { action(); error("Expected IllegalArgumentException") }
            catch (_: IllegalArgumentException) { count++ }
        }
        verify(InvoiceNumbering.format(" a ", 2026, 1) == "A-2026-000001")
        verify(InvoiceNumbering.format("AB_1", 2026, 1000000) == "AB_1-2026-1000000")
        verify(InvoiceNumbering.nextSequence(emptyList(), "A", 2026) == 1L)
        verify(InvoiceNumbering.nextSequence(listOf("A-2026-000001", "A-2026-000003", "B-2026-000099", "A-2025-000100"), "A", 2026) == 4L)
        verify(InvoiceNumbering.nextNumber(listOf("A-2026-000002"), "A", LocalDate.of(2026, 11, 16)) == "A-2026-000003")
        verify(InvoiceNumbering.nextSequence(listOf("A-2026-abc", "A-2026-000000"), "A", 2026) == 1L)
        rejects { InvoiceNumbering.format("", 2026, 1) }
        rejects { InvoiceNumbering.format("a b", 2026, 1) }
        rejects { InvoiceNumbering.format("A", 1999, 1) }
        rejects { InvoiceNumbering.format("A", 2026, 0) }
        rejects { InvoiceNumbering.format("A", 2026, 1000000000) }
        println("PASS: $count InvoiceNumbering checks")
    }
}
