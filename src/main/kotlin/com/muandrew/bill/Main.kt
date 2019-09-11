import com.muandrew.bill.Charge
import com.muandrew.bill.v2.BillV2
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
                processDocument(it)
            }
    } else {
        processDocument(file)
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

fun processDocument(file: File) {
    val text = pdfToString(file) ?: return
    try {
        val charge = stringToCharge(text)
        charge?.printSummary()
    } catch (e: Exception) {
        e.printStackTrace()
        println("Error reading: ${file.name}")
    }
}

fun stringToBuf(pdfFileInText: String): ListIterator<String> {
    // split by whitespace
    val lines = pdfFileInText.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return lines.asList().listIterator()
}

fun stringToCharge(content: String): Charge? {
    return when {
        content.startsWith("Monthly Statement") -> {
            Bill.bufToCharge(stringToBuf(content))
        }
        content.startsWith("Bill period") -> {
            BillV2.parse(stringToBuf(content))
        }
        else -> {
            throw IllegalStateException("Time to write another one!")
        }
    }
}
