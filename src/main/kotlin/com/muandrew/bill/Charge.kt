package com.muandrew.bill

data class Charge(val time: String, val individuals: List<IndividualCharge>, val remainder: Money) {
    fun total(): Money {
        val total = Money.zero()
        individuals.forEach {
            total.addMut(it.total())
        }
        total.addMut(remainder)
        return total
    }

    fun printSummary() {
        println()
        println("Bill: $time")
        println("Total: ${total()}")
        individuals.forEach { it.printSummary() }
    }
}
