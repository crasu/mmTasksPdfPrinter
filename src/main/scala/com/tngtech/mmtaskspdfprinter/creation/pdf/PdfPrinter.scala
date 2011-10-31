package com.tngtech.mmtaskspdfprinter.creation.pdf

import java.io.OutputStream
import java.io.IOException
import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._

class PdfPrinter[T <: OutputStream] (outputStreamConstructor: () => T,
                 val config: PdfConfiguration = PdfConfiguration.defaultConfig) {

  def create(backlogs: List[SprintBacklog]): T = {
    val (doc, outputStream) = setupDocumentAndStream()
    val contentSize = calcContentSize(doc)
    val storyPrinter = new StoryPrinter2(contentSize, config)
    val taskPrinter = new TaskPrinter(contentSize, config)
    val stories = backlogs flatMap {_.stories}
    val storyCards = storyPrinter.create(stories).toList
    val taskCards = taskPrinter.create(stories).toList
    (storyCards ::: taskCards) foreach {page =>
      doc.newPage()
      doc.add(page)
    }
    doc.close()
    outputStream
  }

  private def setupDocumentAndStream() = {
    val doc = new Document(config.pageSize)
    val outputStream = outputStreamConstructor()
    doc.setMargins(8.0f, 8.0f, 8.0f, 8.0f)
    PdfWriter.getInstance(doc, outputStream)
    doc.open()
    (doc, outputStream)
  }

  private def calcContentSize(doc: Document) =
    new Rectangle(
      config.pageSize.getWidth() - doc.leftMargin - doc.rightMargin(),
      config.pageSize.getHeight() - doc.topMargin - doc.bottomMargin())
}
