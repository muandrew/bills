package com.muandrew.bill

interface Condition {

    fun isTrue(line: String): Boolean
}

class Prefix(val prefix: String) : Condition {

    override fun isTrue(line: String): Boolean {
        return line.startsWith(prefix)
    }

    inner class Suffix : Transformer {
        override fun transform(line: String): String {
            return line.substring(prefix.length)
        }
    }
}

class PrefixBuilder(
    private val buf: ListIterator<String>,
    private val consumer: Consumer,
    prefix: String
) {
    private val condition = Prefix(prefix)

    fun withNoTransform(): Builder2 {
        return Builder2(buf, consumer, condition)
    }

    fun withSfxXform(): Builder2 {
        return Builder2(buf, consumer, condition, condition.Suffix())
    }

    fun eom() {
        consumer.consume(buf, condition)
    }
}

class Contains(private val input: String) : Condition {

    override fun isTrue(line: String): Boolean {
        return line.contains(input)
    }
}

class Match(private val match: String) : Condition {

    override fun isTrue(line: String): Boolean {
        return line == match
    }
}