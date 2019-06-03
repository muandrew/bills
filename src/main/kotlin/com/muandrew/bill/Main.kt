import com.muandrew.bill.Money
import com.muandrew.bill.consume
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.PDFTextStripperByArea
import java.io.File


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("please input a file path")
        return
    }
    val fileName = args[0]
    val file = File(fileName)
    if (file.isDirectory) {
        file.listFiles()
            .filter { it.isFile && it.name.endsWith(".pdf") }
            .forEach {
                val text = pdfToString(it) ?: return
                try {
                    stringToBill(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("Error reading: ${it.name}")
                    println("content: ")
                    println(text)
                }
            }
    } else {
        parseDocument(file)
    }
}

fun pdfToString(file: File): String? {
    val doc = PDDocument.load(file)

    if (doc.isEncrypted) {
        println("document encrypted")
        return null
    }

    val stripper = PDFTextStripperByArea()
    stripper.sortByPosition = true

    val tStripper = PDFTextStripper()
    val text = tStripper.getText(doc)
    doc.close()
    return text
}

fun stringToBill(pdfFileInText: String): Bill {
    // split by whitespace
    val lines = pdfFileInText.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val buf = lines.asList().listIterator()

    val statementFor = buf.consume().to().prefix("Statement for: ").withSfxXform().rtnLastMatch()
    val accountNumber = buf.consume().to().prefix("Account number: ").withSfxXform().rtnLastMatch()
    buf.consume().to().match("Total amount due").eom()
    val dueDate = buf.consume().anExpected().prefix("by ").withSfxXform().rtnLastMatch()
    val amount = Money.parse(buf.next())
    buf.consume().to().match("Balance").eom()
    buf.consume().anExpected().match("Current charges").eom()
    val recurringAmount = Money.parse(buf.consume().anExpected().prefix("Recurring ").withSfxXform().rtnLastMatch()!!)
    val otherAmount = Money.parse(buf.consume().anExpected().prefix("Other ").withSfxXform().rtnLastMatch()!!)
    buf.consume().to().match("Current charges").eom()
    buf.consume().anExpected().match("Account and lines Recurring Other").eom()
    buf.consume().anExpected().match("Change from").eom()
    buf.consume().anExpected().match("last month").eom()

    val bill = Bill.create(buf)

    assert(amount == recurringAmount.add(otherAmount))
    assert(amount == bill.total)

    val pooledCost = Money.zero()
    pooledCost.addMut(bill.accountLineItem.computedTotal())

    val flattenItems = mutableListOf<FlatItem>()
    val accToCost = mutableMapOf<String, MutableList<FlatItem>>()
    bill.accountDetails.forEach {
        accToCost[it.lineItem.phoneLine] = mutableListOf()
        it.flatten(flattenItems, it.lineItem.phoneLine)
    }

    val additionalAccountCost = mutableListOf<FlatItem>()
    val phoneLineCost = mutableListOf<FlatItem>()

    flattenItems.forEach {
        if (it.description.contains("AutoPay Discount") || it.description.contains("T-Mobile ONE")) {
            additionalAccountCost.add(it)
        } else {
            phoneLineCost.add(it)
        }
    }

    additionalAccountCost.forEach { pooledCost.addMut(it.money) }

    val totalIndividualCost = Money.zero()

    phoneLineCost.forEach {
        totalIndividualCost.addMut(it.money)
        accToCost[it.phoneLine]!!.add(it)
    }

    assert(pooledCost.add(totalIndividualCost) == bill.total)

    println()
    println(accToCost)
    println()
    println("bill: ${bill.total}")
    println("pooledCost: $pooledCost")
    println("totalIndividualCost: $totalIndividualCost")
    println()

    val individualPooledCost = pooledCost.copy()
    val remainder = individualPooledCost.divideMut(4)
    accToCost.forEach {
        val phoneLine = it.key
        val cost = it.value.sumFlatItems()
        val individualTotal = individualPooledCost.add(cost)
        println("$phoneLine: $individualPooledCost + $cost = $individualTotal")
    }
    println("remainder: $remainder")
    return bill
}

fun parseDocument(file: File): Bill? {
    val pdfFileInText = pdfToString(file) ?: return null
    return stringToBill(pdfFileInText)
}

data class FlatItem(val phoneLine: String, val type: Section.Type, val description: String, val money: Money)

fun List<FlatItem>.sumFlatItems(): Money {
    val acc = Money.zero()
    this.forEach {
        acc.addMut(it.money)
    }
    return acc
}

fun List<Section.Item>.sumSectionItems(): Money {
    val acc = Money.zero()
    this.forEach {
        acc.addMut(it.money)
    }
    return acc
}
