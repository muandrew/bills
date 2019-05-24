package com.muandrew.bill

fun ListIterator<String>.consume(): Builder0 {
    return Builder0(this)
}

class Builder0(val buf: ListIterator<String>) {

    fun anExpected(): Builder1 {
        return Builder1(Expected())
    }

    fun to(): Builder1 {
        return Builder1(To())
    }

    fun until(): Builder1 {
        return Builder1(Until())
    }

    fun while_(): Builder1 {
        return Builder1(While())
    }

    inner class Builder1(val consumer: Consumer) {

        fun contains(input: String): Builder2 {
            return Builder2(buf, consumer, Contains(input))
        }

        fun match(match: String): Builder2 {
            return Builder2(buf, consumer, Match(match))
        }

        fun prefix(prefix: String): PrefixBuilder {
            return PrefixBuilder(buf, consumer, prefix)
        }
    }
}

open class Builder2(
    private val buf: ListIterator<String>,
    private val consumer: Consumer,
    private val condition: Condition,
    private val transformer: Transformer? = null
) {

    fun returnAllDiscarded(): List<String> {
        val storage = AllDiscarded()
        consumer.consume(buf, StorageAdapter(condition, storage, transformer))
        return storage.result
    }

    fun rtnLastMatch(): String? {
        val storage = LastMatch()
        consumer.consume(buf, StorageAdapter(condition, storage, transformer))
        return storage.result
    }

    fun returnAllMatches(): List<String> {
        val storage = AllMatches()
        consumer.consume(buf, StorageAdapter(condition, storage, transformer))
        return storage.result
    }

    fun returnAllTouched(): List<String> {
        val storage = AllTouched()
        consumer.consume(buf, StorageAdapter(condition, storage, transformer))
        return storage.result
    }

    fun eom() {
        consumer.consume(buf, condition)
    }
}

class StorageAdapter(
    private val condition: Condition,
    private val storage: Storage,
    private val transformer: Transformer?) : Condition {

    override fun isTrue(line: String): Boolean {
        val isTrue = condition.isTrue(line)
        var finalLine = line
        if (isTrue && transformer != null) {
            finalLine = transformer.transform(line)
        }
        storage.store(finalLine, isTrue)
        return isTrue
    }
}
