package com.muandrew.bill

interface Storage {

    fun store(line: String, isCondition: Boolean)
}

class LastMatch : Storage {
    var result: String? = null

    override fun store(line: String, isCondition: Boolean) {
        if (isCondition) {
            result = line
        }
    }
}

class AllMatches : Storage {
    var result: MutableList<String> = mutableListOf()

    override fun store(line: String, isCondition: Boolean) {
        if (isCondition) {
            result.add(line)
        }
    }
}

class AllTouched: Storage {
    var result: MutableList<String> = mutableListOf()

    override fun store(line: String, isCondition: Boolean) {
        result.add(line)
    }
}

class AllDiscarded: Storage {
    var result: MutableList<String> = mutableListOf()

    override fun store(line: String, isCondition: Boolean) {
        if (!isCondition) {
            result.add(line)
        }
    }
}
