import com.muandrew.bill.LineTotal
import com.muandrew.bill.Money
import com.muandrew.bill.consume

data class AccountDetails(
    val lineItem: LineTotal,
    val plan: Section?,
    val equipment: Section?,
    val sectionsTotal: Money
) {

    companion object {
        fun createAccountDetails(buf: ListIterator<String>, lineTotal: LineTotal): AccountDetails {
            if (lineTotal.computedTotal().cents == 0L) {
                return AccountDetails(lineTotal, null, null, Money.zero())
            }

            buf.consume().to().match(lineTotal.name).eom()
            buf.consume().to().prefix("Service from ").eom()
            var planSection: Section? = null
            var section = buf.next()
            var equipmentSection: Section? = null
            var sectionsTotal: Money? = null
            while (true) {
                if (section == "Plan") {
                    planSection = Section.processSection(buf)
                } else if (section == "Equipment") {
                    equipmentSection = Section.processSection(buf)
                } else if (section.startsWith("Total")) {
                    sectionsTotal = Money.parse(section.substring("Total: ".length))
                    break
                } else {
                    break
                }
                section = buf.next()
            }
            val result = AccountDetails(lineTotal, planSection, equipmentSection, sectionsTotal!!)
            result.validate()
            return result
        }
    }

    fun validate() {
        val planTotal = plan?.subtotal ?: Money.zero()
        val equipmentTotal = equipment?.subtotal ?: Money.zero()
        if (sectionsTotal != lineItem.computedTotal() ||
            sectionsTotal != planTotal.add(equipmentTotal)
        ) {
            throw IllegalStateException("Yo monies don't add up.")
        }
    }
}