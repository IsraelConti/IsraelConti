package tec.mmhi.facturas

import java.math.BigDecimal
import java.math.RoundingMode

data class InvoiceTotals(val base: BigDecimal, val tax: BigDecimal, val total: BigDecimal)

object InvoiceCalculator {
    fun calculate(quantity: String, unitPrice: String, vatPercent: String): InvoiceTotals {
        val q = BigDecimal(quantity.replace(',', '.'))
        val price = BigDecimal(unitPrice.replace(',', '.'))
        val vat = BigDecimal(vatPercent.replace(',', '.'))
        require(q > BigDecimal.ZERO && price >= BigDecimal.ZERO && vat >= BigDecimal.ZERO && vat <= BigDecimal("100"))
        val base = q.multiply(price).setScale(2, RoundingMode.HALF_UP)
        val tax = base.multiply(vat).divide(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
        return InvoiceTotals(base, tax, base.add(tax))
    }
}
