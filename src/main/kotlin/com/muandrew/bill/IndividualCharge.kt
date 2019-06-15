package com.muandrew.bill

data class IndividualCharge(val individual: String, val plan: Money, val modifier: Money) {
    fun total(): Money {
        return plan.add(modifier)
    }

    fun printSummary() {
        println("$individual: $plan + $modifier = ${total()}")
    }
}