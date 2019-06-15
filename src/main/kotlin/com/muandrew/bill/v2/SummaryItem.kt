package com.muandrew.bill.v2

import com.muandrew.bill.Money

data class SummaryItem(
    val individual: String,
    val plans: Money,
    val equipment: Money,
    val services: Money,
    val oneTime: Money,
    val total: Money
) {
    companion object {
        private const val normal = "Plans Equipment Services Total"
        private const val oneTime = "Plans Equipment Services One-time charges Total"

        fun parse(header: String, line: String): SummaryItem {
            val hasOnetime = when (header) {
                normal -> {
                    false
                }
                oneTime -> {
                    true
                }
                else -> {
                    throw IllegalStateException("Unknown header type")
                }
            }
            val itr = line.split(" ").listIterator()
            return SummaryItem(
                itr.next(),
                Money.parse(itr.next()),
                Money.parse(itr.next()),
                Money.parse(itr.next()),
                if (hasOnetime) {
                    Money.parse(itr.next())
                } else {
                    Money.zero()
                },
                Money.parse(itr.next())
            )
        }
    }
}