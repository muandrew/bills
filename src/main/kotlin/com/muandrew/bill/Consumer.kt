package com.muandrew.bill

interface Consumer {
    fun consume(buf: ListIterator<String>, condition: Condition)
}

class Expected : Consumer {
    override fun consume(buf: ListIterator<String>, condition: Condition) {
        if (!buf.hasNext()) {
            val line = buf.previous()
            throw java.lang.IllegalStateException("Unexpected end, prev line: $line")
        }
        val line = buf.next()
        if (!condition.isTrue(line)) {
            throw IllegalStateException("Unexpected: $line.")
        }
    }
}

class To : Consumer {
    override fun consume(buf: ListIterator<String>, condition: Condition) {
        while (buf.hasNext()) {
            val result = condition.isTrue(buf.next())
            if (result) {
                return
            }
        }
    }
}

class Until : Consumer {

    override fun consume(buf: ListIterator<String>, condition: Condition) {
        while (buf.hasNext()) {
            val result = condition.isTrue(buf.next())
            if (result) {
                buf.previous()
                return
            }
        }
    }
}

class While : Consumer {
    override fun consume(buf: ListIterator<String>, condition: Condition) {
        while (buf.hasNext()) {
            val result = condition.isTrue(buf.next())
            if (!result) {
                buf.previous()
                return
            }
        }
    }
}