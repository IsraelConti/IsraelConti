package tec.mmhi.facturas

import java.math.BigDecimal
import java.math.RoundingMode

/** Cálculo monetario determinista, independiente de Android y de la interfaz. */
data class InvoiceLine(
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val vatPercent: BigDecimal
)

data class CalculatedInvoiceLine(
    val description: String,
    val base: BigDecimal,
    val vat: BigDecimal,
    val total: BigDecimal,
    val vatPercent: BigDecimal
)

data class VatBreakdown(
    val percent: BigDecimal,
    val taxableBase: BigDecimal,
    val tax: BigDecimal
)

data class CalculatedInvoice(
    val lines: List<CalculatedInvoiceLine>,
    val vatBreakdown: List<VatBreakdown>,
    val base: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal
)

object InvoiceDocumentCalculator {
    private val ZERO = BigDecimal("0.00")
    private val HUNDRED = BigDecimal("100")

    fun calculate(lines: List<InvoiceLine>): CalculatedInvoice {
        require(lines.isNotEmpty()) { "La factura necesita al menos una línea" }
        val calculated = lines.map { line ->
            require(line.description.isNotBlank()) { "Descripción vacía" }
            require(line.quantity > BigDecimal.ZERO) { "Cantidad no positiva" }
            require(line.unitPrice >= BigDecimal.ZERO) { "Precio negativo" }
            require(line.vatPercent >= BigDecimal.ZERO && line.vatPercent <= HUNDRED) { "IVA fuera de rango" }
            val base = money(line.quantity.multiply(line.unitPrice))
            val vat = money(base.multiply(line.vatPercent).divide(HUNDRED))
            CalculatedInvoiceLine(line.description.trim(), base, vat, base.add(vat), line.vatPercent.stripTrailingZeros())
        }
        val breakdown = calculated.groupBy { it.vatPercent }
            .toSortedMap()
            .map { (percent, group) ->
                VatBreakdown(percent, group.fold(ZERO) { acc, item -> acc.add(item.base) },
                    group.fold(ZERO) { acc, item -> acc.add(item.vat) })
            }
        val base = breakdown.fold(ZERO) { acc, item -> acc.add(item.taxableBase) }
        val tax = breakdown.fold(ZERO) { acc, item -> acc.add(item.tax) }
        return CalculatedInvoice(calculated, breakdown, base, tax, base.add(tax))
    }

    private fun money(value: BigDecimal) = value.setScale(2, RoundingMode.HALF_UP)
}
