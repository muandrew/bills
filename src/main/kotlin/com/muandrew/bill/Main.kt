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

    val doc = PDDocument.load(File(fileName))

    if (doc.isEncrypted) {
        println("document encrypted")
        return
    }

    val stripper = PDFTextStripperByArea()
    stripper.sortByPosition = true

    val tStripper = PDFTextStripper()

    val pdfFileInText = tStripper.getText(doc)

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

    println("$bill")
}

fun List<Section.Item>.sum(): Money {
    val acc = Money.zero()
    this.forEach {
        acc.addMut(it.money)
    }
    return acc
}
