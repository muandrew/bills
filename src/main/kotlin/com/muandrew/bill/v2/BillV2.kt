package com.muandrew.bill.v2

import com.muandrew.bill.Charge
import com.muandrew.bill.IndividualCharge
import com.muandrew.bill.Money
import com.muandrew.bill.consume

class BillV2 {

    companion object {
        private val mainAccountIndex = 3
        private val zero = Money.zero()
        private val ten = Money.parse("$10")
        private val twenty = Money.parse("$20")
        private val twentyfive = Money.parse("$25")
        private val three = Money.parse("$3")
        private val five = Money.parse("$5")

        fun parse(buf: ListIterator<String>): Charge {

            buf.consume().anExpected().match("Bill period").eom()
            val billPeriod = buf.next()
            buf.consume().anExpected().match("Account").eom()
            val account = buf.next()
            buf.consume().to().match("TOTAL DUE").eom()
            val total = Money.parse(buf.next())
            val header = buf.consume().to().prefix("Plans Equipment Services").withNoTransform().rtnLastMatch()!!
            val summaryLines = buf.consume().until().match("DETAILED CHARGES").returnAllDiscarded()
            val summaryItems = summaryLines.map { SummaryItem.parse(header, it) }

            val totalPlan = Money.zero()
            val credit = mutableMapOf<String, Money>()
            val additionalCosts = mutableMapOf<String, Money>()
            summaryItems.forEach {
                val plan = it.plans
                when {
                    it.individual.startsWith("Account") -> {
                        totalPlan.addMut(plan)

                        // additional service cost to main account
//                        val service = it.services
//                        when (service) {
//                            three,five -> {
//                                additionalCosts = service
//                            }
//                        }
                    }
                    it.individual.startsWith("Total") -> {
                    }
                    it.individual.startsWith("(") -> when (plan) {
                        twenty, twentyfive -> {
                            totalPlan.addMut(plan)
                        }
                        ten -> {
                            totalPlan.addMut(twenty)
                            credit[it.individual] = plan.subtract(twenty)
                        }
                        zero -> {
                        }
                        else -> {
                            throw IllegalStateException("Is this valid?")
                        }
                    }
                    else -> throw IllegalStateException("who are you?")
                }
            }
            val individualPlan = totalPlan.copy()
            val planRemainder = individualPlan.divideMut((summaryItems.size - 2).toLong())
            val individuals = summaryItems
                .filter { it.individual.startsWith("(") }
                .map {
                    IndividualCharge(
                        it.individual,
                        individualPlan,
                        credit.getOrDefault(
                            it.individual,
                            Money.zero()
                        ).add(it.equipment).add(it.services).add(it.oneTime)
                            .add(additionalCosts.getOrDefault(it.individual, Money.zero()))
                    )
                }

            val charge = Charge(billPeriod, individuals, planRemainder)
            if (charge.total() != total) {
                throw IllegalStateException("Something isn't adding up. Charge = " + charge.total() + " Total = " + total)
            }
            return charge
        }
    }
}
