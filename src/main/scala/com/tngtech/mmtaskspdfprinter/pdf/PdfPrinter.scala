package com.tngtech.mmtaskspdfprinter.pdf

import java.io.OutputStream
import java.io.IOException
import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf.config._

class PdfPrinter(val outputStream: OutputStream, val config: Configuration) {
  private val doc = new Document(config.pageSize)
  doc.setMargins(8.0f, 8.0f, 8.0f, 8.0f)
  private val contentSize = new Rectangle(
    config.pageSize.getWidth() - doc.leftMargin - doc.rightMargin(),
    config.pageSize.getHeight() - doc.topMargin - doc.bottomMargin())

  PdfWriter.getInstance(doc, outputStream)
  doc.open()

  private var storiesToPrint = List[Story]()

  def numberOfStories() = storiesToPrint.size

  def addSprintBacklog(backlog: SprintBacklog) = storiesToPrint ++= backlog.stories

  def toFile() {
    if (storiesToPrint.size == 0) {
      throw new PrinterException("No stories given")
    }
    createTaskPages()
    doc.close()
  }

  private def createTaskPages() {
    var storyPrinter = new StoryPrinter(contentSize, config)
    var taskPrinter = new TaskPrinter(contentSize, config)
    storiesToPrint.foreach(story => {
        storyPrinter.addStory(story)
        taskPrinter.addStory(story)
    })
    var pages = storyPrinter.printPages()
    pages ++= taskPrinter.printPages()
    pages.foreach(page => {
      doc.newPage()
      doc.add(page)
    })
  }
}
