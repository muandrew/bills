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
                    Bill.stringToBill(text)
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


fun parseDocument(file: File): Bill? {
    val pdfFileInText = pdfToString(file) ?: return null
    return Bill.stringToBill(pdfFileInText)
}
