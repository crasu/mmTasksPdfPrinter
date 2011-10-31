package com.tngtech.mmtaskspdfprinter.creation.pdf

import java.io.OutputStream
import java.io.IOException
import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._
import java.io.ByteArrayOutputStream

class PdfPrinter (
    val config: PdfConfiguration = PdfConfiguration.defaultConfig) {

  def create(backlogs: List[SprintBacklog], out:OutputStream):Unit = {
    val doc = {
    val doc = new Document(config.pageSize)
    doc.setMargins(8.0f, 8.0f, 8.0f, 8.0f)
    PdfWriter.getInstance(doc, out)
    doc.open()
    doc
  }
    val contentSize = calcContentSize(doc)
    val storyPrinter = new StoryPrinter(contentSize, config)
    val taskPrinter = new TaskPrinter(contentSize, config)
    val stories = backlogs flatMap {_.stories}
    val storyCards = storyPrinter.create(stories).toList
    val taskCards = taskPrinter.create(stories).toList
    (storyCards ::: taskCards) foreach {page =>
      doc.newPage()
      doc.add(page)
    }
    doc.close()
  }
  
  def create(backlogs: List[SprintBacklog]):Array[Byte] = {
    val stream = new ByteArrayOutputStream();
    create(backlogs, stream)
    stream.toByteArray();
  }

  private def calcContentSize(doc: Document) =
    new Rectangle(
      config.pageSize.getWidth() - doc.leftMargin - doc.rightMargin(),
      config.pageSize.getHeight() - doc.topMargin - doc.bottomMargin())
}
