package tec.mmhi.facturas

import java.math.BigDecimal

/** Standalone regression suite. Run with kotlinc and java; no Android SDK needed. */
object InvoiceDocumentCalculatorRegressionTest {
    @JvmStatic fun main(args: Array<String>) {
        fun d(s: String) = BigDecimal(s)
        fun line(q: String = "1", p: String = "10", vat: String = "21", desc: String = "Producto") =
            InvoiceLine(desc, d(q), d(p), d(vat))
        var assertions = 0
        fun verify(condition: Boolean, label: String) {
            check(condition) { label }
            assertions++
        }
        fun rejects(label: String, action: () -> Unit) {
            try { action(); error("No se rechazó: $label") }
            catch (_: IllegalArgumentException) { assertions++ }
        }
        val simple = InvoiceDocumentCalculator.calculate(listOf(line()))
        verify(simple.base == d("10.00"), "base simple")
        verify(simple.tax == d("2.10"), "IVA simple")
        verify(simple.total == d("12.10"), "total simple")
        verify(simple.lines.size == 1, "una línea")
        verify(simple.vatBreakdown.size == 1, "un tipo IVA")
        val mixed = InvoiceDocumentCalculator.calculate(listOf(line("2", "10", "21"), line("3", "5", "10"), line("1", "7", "0")))
        verify(mixed.base == d("42.00"), "base mixta")
        verify(mixed.tax == d("5.70"), "IVA mixto")
        verify(mixed.total == d("47.70"), "total mixto")
        verify(mixed.vatBreakdown.map { it.percent }.zip(listOf(d("0"), d("10"), d("21"))).all { (a, b) -> a.compareTo(b) == 0 }, "desglose ordenado")
        verify(mixed.vatBreakdown.map { it.tax } == listOf(d("0.00"), d("1.50"), d("4.20")), "impuestos por tipo")
        val rounded = InvoiceDocumentCalculator.calculate(listOf(line("3", "0.335", "21")))
        verify(rounded.base == d("1.01"), "redondeo base HALF_UP")
        verify(rounded.tax == d("0.21"), "redondeo IVA HALF_UP")
        verify(rounded.total == d("1.22"), "total redondeado")
        val free = InvoiceDocumentCalculator.calculate(listOf(line("1", "0", "0")))
        verify(free.total == d("0.00"), "precio cero permitido")
        val normalized = InvoiceDocumentCalculator.calculate(listOf(line(desc="  Servicio  ")))
        verify(normalized.lines.single().description == "Servicio", "recorte de descripción")
        val grouped = InvoiceDocumentCalculator.calculate(listOf(line("1", "1", "21"), line("1", "2", "21.0")))
        verify(grouped.vatBreakdown.size == 1, "agrupación IVA con escala distinta")
        verify(grouped.vatBreakdown.single().taxableBase == d("3.00"), "base agrupada")
        rejects("vacía") { InvoiceDocumentCalculator.calculate(emptyList()) }
        rejects("descripción") { InvoiceDocumentCalculator.calculate(listOf(line(desc="  "))) }
        rejects("cantidad cero") { InvoiceDocumentCalculator.calculate(listOf(line(q="0"))) }
        rejects("cantidad negativa") { InvoiceDocumentCalculator.calculate(listOf(line(q="-1"))) }
        rejects("precio negativo") { InvoiceDocumentCalculator.calculate(listOf(line(p="-0.01"))) }
        rejects("IVA negativo") { InvoiceDocumentCalculator.calculate(listOf(line(vat="-1"))) }
        rejects("IVA superior 100") { InvoiceDocumentCalculator.calculate(listOf(line(vat="100.01"))) }
        println("PASS: $assertions verificaciones de regresión JVM")
    }
}
