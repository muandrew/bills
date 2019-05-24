import com.muandrew.bill.Money
import com.muandrew.bill.consume

data class Section(
    val items: List<Item>,
    val remainder: List<Item>,
    val breakdown: List<Item>,
    val subtotal: Money
) {

    companion object {

        fun processSection(buf: ListIterator<String>): Section {
            val planItems = mutableListOf<Item>()
            var remainderItems: List<Item>? = null
            while (buf.hasNext()) {
                val line = buf.next()
                if (line == "Breakdown") {
                    break
                } else if (line == "Description Balance of remaining payments") {
                    remainderItems = buf.consume()
                        .until()
                        .prefix("Breakdown")
                        .withNoTransform()
                        .returnAllDiscarded()
                        .filter { it.contains("$") }
                        .map { Section.Item.parse(it) }
                    break
                } else {
                    if (line.contains("$")) {
                        planItems.add(Section.Item.parse(line))
                    }
                }
            }
            val planBreakdown = buf.consume()
                .until()
                .prefix("Subtotal: ")
                .withNoTransform()
                .returnAllDiscarded()
                .filter { it.contains("$") }
                .map { Section.Item.parse(it) }
            val subtotalStr = buf.consume().anExpected().prefix("Subtotal: ").withSfxXform().rtnLastMatch()
            val section = Section(
                planItems,
                remainderItems ?: listOf(),
                planBreakdown,
                Money.parse(subtotalStr!!)
            )
            section.validate()
            return section
        }
    }

    private fun validate() {
        val itemsSum = items.sum()
        if (subtotal != itemsSum) {
            throw IllegalStateException("Yo monies don't add up.")
        }
    }

    data class Item(val description: String, val money: Money) {

        companion object {
            fun parse(line: String): Item {
                val tokens = line.split(" ")
                return Item(
                    tokens.subList(0, tokens.lastIndex - 1).joinToString(" "),
                    Money.parse(tokens[tokens.lastIndex])
                )
            }
        }
    }
}