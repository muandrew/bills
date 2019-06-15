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

        fun stringToBill(pdfFileInText: String): Bill {
            // split by whitespace
            val lines = pdfFileInText.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val buf = lines.asList().listIterator()

            val statementFor = buf.consume().to().prefix("Statement for: ").withSfxXform().rtnLastMatch()
            val accountNumber = buf.consume().to().prefix("Account number: ").withSfxXform().rtnLastMatch()
            buf.consume().to().match("Total amount due").eom()
            val dueDate = buf.consume().anExpected().prefix("by ").withSfxXform().rtnLastMatch()
            val amount = Money.parse(buf.next())
            buf.consume().to().match("Balance").eom()
            buf.consume().anExpected().match("Current charges").eom()
            val recurringAmount =
                Money.parse(buf.consume().anExpected().prefix("Recurring ").withSfxXform().rtnLastMatch()!!)
            val otherAmount = Money.parse(buf.consume().anExpected().prefix("Other ").withSfxXform().rtnLastMatch()!!)
            buf.consume().to().match("Current charges").eom()
            buf.consume().anExpected().match("Account and lines Recurring Other").eom()
            buf.consume().anExpected().match("Change from").eom()
            buf.consume().anExpected().match("last month").eom()

            val bill = Bill.create(buf)

            assert(amount == recurringAmount.add(otherAmount))
            assert(amount == bill.total)

            val pooledCost = Money.zero()
            pooledCost.addMut(bill.accountLineItem.computedTotal())

            val flattenItems = mutableListOf<FlatItem>()
            val accToCost = mutableMapOf<String, MutableList<FlatItem>>()
            bill.accountDetails.forEach {
                accToCost[it.lineItem.phoneLine] = mutableListOf()
                it.flatten(flattenItems, it.lineItem.phoneLine)
            }

            val additionalAccountCost = mutableListOf<FlatItem>()
            val phoneLineCost = mutableListOf<FlatItem>()

            flattenItems.forEach {
                if (it.description.contains("AutoPay Discount") || it.description.contains("T-Mobile ONE")) {
                    additionalAccountCost.add(it)
                } else {
                    phoneLineCost.add(it)
                }
            }

            additionalAccountCost.forEach { pooledCost.addMut(it.money) }

            val totalIndividualCost = Money.zero()

            phoneLineCost.forEach {
                totalIndividualCost.addMut(it.money)
                accToCost[it.phoneLine]!!.add(it)
            }

            assert(pooledCost.add(totalIndividualCost) == bill.total)

            println()
            println(accToCost)
            println()
            println("bill: ${bill.total}")
            println("pooledCost: $pooledCost")
            println("totalIndividualCost: $totalIndividualCost")
            println()

            val individualPooledCost = pooledCost.copy()
            val remainder = individualPooledCost.divideMut(4)
            accToCost.forEach {
                val phoneLine = it.key
                val cost = it.value.sumFlatItems()
                val individualTotal = individualPooledCost.add(cost)
                println("$phoneLine: $individualPooledCost + $cost = $individualTotal")
            }
            println("remainder: $remainder")
            return bill
        }

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

data class FlatItem(val phoneLine: String, val type: Section.Type, val description: String, val money: Money)

fun List<FlatItem>.sumFlatItems(): Money {
    val acc = Money.zero()
    this.forEach {
        acc.addMut(it.money)
    }
    return acc
}

fun List<Section.Item>.sumSectionItems(): Money {
    val acc = Money.zero()
    this.forEach {
        acc.addMut(it.money)
    }
    return acc
}