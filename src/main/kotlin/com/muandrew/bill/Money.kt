package com.muandrew.bill

data class Money(var cents: Long = 0) {

    companion object {

        val moneyParser = "(\\(\\$(.*)\\)|\\$(.*))".toRegex()

        private fun dollarsToCents(input: String): Long {
            return (input.toDouble() * 100).toLong()
        }

        fun parse(input: String): Money {
            val result = moneyParser.matchEntire(input) ?: return Money()
            val results = result.groupValues
            val negative = results.getOrNull(2)
            if (!negative.isNullOrEmpty()) {
                return Money(-1 * dollarsToCents(negative))
            }
            val positive = results.getOrNull(3)
            return if (!positive.isNullOrEmpty()) {
                Money(dollarsToCents(positive))
            } else {
                Money()
            }
        }

        fun fromDollars(dollars: Long): Money {
            return Money(dollars * 100)
        }

        fun zero(): Money {
            return Money(0)
        }
    }

    override fun toString(): String {
        var dollars = cents.toDouble() / 100f
        if (dollars < 0) {
            dollars *= -1;
            return "($$dollars)"
        }
        return "$$dollars"
    }

    fun add(money: Money): Money {
        return Money(cents + money.cents)
    }

    fun subtract(money: Money): Money {
        return Money(cents - money.cents)
    }

    fun addMut(money: Money) {
        cents += money.cents
    }

    fun multiply(multiple: Long): Money {
        return Money(cents * multiple)
    }

    fun divideMut(denominator: Long): Money {
        val remainder = cents % denominator
        cents /= denominator
        return Money(remainder)
    }
}