import com.muandrew.bill.LineTotal
import com.muandrew.bill.Money
import com.muandrew.bill.consume

data class Bill(
    val accountLineItem: LineTotal,
    val accountDetails: List<AccountDetails>,
    val subtotalRecurring: Money,
    val subtotalOther: Money,
    val total: Money
) {

    fun validate() {
        val recurringTotal = Money.zero()
        recurringTotal.addMut(accountLineItem.recurring)
        val otherTotal = Money.zero()
        otherTotal.addMut(accountLineItem.other)
        accountDetails.forEach {
            recurringTotal.addMut(it.lineItem.recurring)
            otherTotal.addMut(it.lineItem.other)
        }
        if (total != subtotalRecurring.add(subtotalOther)
            || recurringTotal != subtotalRecurring
            || otherTotal != subtotalOther
        ) {
            throw IllegalStateException("No add up.")
        }
    }

    companion object {
        fun create(buf: ListIterator<String>): Bill {
            val lineTotals = buf.consume()
                .until()
                .prefix("Subtotal")
                .withNoTransform()
                .returnAllDiscarded().map { LineTotal.parse(it) }

            val subtotalTokens = buf.next().split(" ")
            val subTotalRecurring = Money.parse(subtotalTokens[1])
            val subTotalOther = Money.parse(subtotalTokens[2])
            val total =
                Money.parse(buf.consume().anExpected().prefix("Total ").withSfxXform().rtnLastMatch()!!)

            val itr = lineTotals.iterator();
            val accountLineItem = itr.next()

            val accountDetailsList = mutableListOf<AccountDetails>()
            while (itr.hasNext()) {
                val lineTotal = itr.next()
                accountDetailsList.add(AccountDetails.createAccountDetails(buf, lineTotal))
            }
            val result = Bill(accountLineItem, accountDetailsList, subTotalRecurring, subTotalOther, total)
            result.validate()
            return result
        }
    }
}