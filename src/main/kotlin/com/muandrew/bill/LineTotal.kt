package com.muandrew.bill

data class LineTotal(val name: String, val recurring: Money, val other: Money) {
    companion object {
        fun parse(row: String): LineTotal {
            val column = row.split(" ")
            return LineTotal(
                column.subList(0, column.lastIndex - 2).joinToString(" "),
                Money.parse(column[column.lastIndex - 2]),
                Money.parse(column[column.lastIndex - 1])
            )
        }
    }

    fun computedTotal(): Money {
        return recurring.add(other)
    }
}
