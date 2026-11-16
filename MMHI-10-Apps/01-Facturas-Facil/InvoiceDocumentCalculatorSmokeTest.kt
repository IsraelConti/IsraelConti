package tec.mmhi.facturas

import java.math.BigDecimal

/** Ejecutar con kotlinc InvoiceDocumentCalculator.kt InvoiceDocumentCalculatorSmokeTest.kt -include-runtime -d test.jar && java -jar test.jar */
object InvoiceDocumentCalculatorSmokeTest {
    @JvmStatic
    fun main(args: Array<String>) {
        fun n(s: String) = BigDecimal(s)
        val invoice = InvoiceDocumentCalculator.calculate(listOf(
            InvoiceLine("Producto", n("2"), n("10.00"), n("21")),
            InvoiceLine("Servicio", n("1"), n("5.00"), n("10"))
        ))
        check(invoice.base == n("25.00"))
        check(invoice.tax == n("4.70"))
        check(invoice.total == n("29.70"))
        check(invoice.vatBreakdown.size == 2)
        try {
            InvoiceDocumentCalculator.calculate(emptyList())
            error("Debe rechazar facturas sin líneas")
        } catch (_: IllegalArgumentException) {
            // Validación esperada
        }
        println("PASS: 5 verificaciones JVM")
    }
}
