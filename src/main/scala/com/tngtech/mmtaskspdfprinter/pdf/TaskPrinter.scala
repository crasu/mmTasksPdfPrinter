package com.tngtech.mmtaskspdfprinter.pdf

import com.itextpdf.text._
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import scala.List

object TaskPrinter {
  private val columnSize = 3
  private val rowSize = 4
  private val noOfElementsPerPage = columnSize * rowSize
  private val maxNoOfSubtasks = 5
  private val square = {
    /*
    * FreeSerif.ttf is provided by http://savannah.gnu.org/projects/freefont/
    */
    val baseFontSymbol = BaseFont.createFont("/FreeSerif.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    val symbolFont = new Font(baseFontSymbol, 10)
    new Chunk("\u2752", symbolFont)
  }
}

class TaskPrinter(contentSize: Rectangle) extends PagePrinter(contentSize) {
  override def addStory(story: Story) {
    story.tasks.foreach(addTask(story, _))
  }

  protected def addTask(story: Story, task: Task) {
    if (noOfElements % TaskPrinter.noOfElementsPerPage == 0) {
      addNewPage()
    }

    val cell = createCell(story, task)
    pages.last.addCell(cell)

    noOfElements += 1
  }

  private def addNewPage() {
    val page = new PdfPTable(TaskPrinter.columnSize)
    page.setWidthPercentage(100)
    pages += page
  }

  private def createCell(story: Story, task: Task) = {
    val cell = createFramingCell()
    var content = taskToPhrase(story, task)
    createInnerTable(cell, content)
    cell
  }

  private def createFramingCell() = {
    val cell = new PdfPCell()
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(contentSize.getHeight() / TaskPrinter.rowSize)
    cell
  }

  private def taskToPhrase(story: Story, task: Task) = {
    val phrase = new Phrase()
    phrase.add(new Chunk(story.name + "\n", PagePrinter.normalFont))
    phrase.add(new Chunk(task.category + "\n", PagePrinter.smallFont))
    phrase.add(new Chunk(task.description + "\n", PagePrinter.bigFont))

    task.subtasks.take(TaskPrinter.maxNoOfSubtasks - 1).foreach {subtask =>
      phrase.add(TaskPrinter.square)
      phrase.add(new Chunk(" " + subtask.description + "\n", PagePrinter.normalFont))
    }
    val remainingTasks = task.subtasks.drop(TaskPrinter.maxNoOfSubtasks - 1)
    if (!remainingTasks.isEmpty) {
      val descriptions = remainingTasks.map(_.description)
      val concated = descriptions.mkString(", ")
      phrase.add(TaskPrinter.square)
      phrase.add(new Chunk(" " + concated + "\n", PagePrinter.normalFont))
    }
    phrase
  }

  private def createInnerTable(outerCell: PdfPCell, phrase: Phrase) = {
    var table = new PdfPTable(1)
    table.setWidthPercentage(100.0f)
    var cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(5.0f)
    cell.setIndent(0)
    cell.addElement(phrase)
    cell.setFixedHeight(outerCell.getFixedHeight() - 3)
    cell.setFixedHeight(outerCell.getFixedHeight() - PagePrinter.companyLogo.getScaledHeight - 3)
    table.addCell(cell)
    
    cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setPaddingLeft(5.0f)
    cell.setIndent(0)
    cell.addElement(PagePrinter.companyLogo)
    cell.setFixedHeight(PagePrinter.companyLogo.getScaledHeight)
    table.addCell(cell)

    outerCell.addElement(table)
  }

  override def printPages() = {
    fillWithEmptyCells
    pages
  }

  private def fillWithEmptyCells() {
    val emptyStory = Story("")
    val emptyTask = Task("", "")
    while (noOfElements % TaskPrinter.noOfElementsPerPage != 0) {
      addTask(emptyStory, emptyTask)
    }
  }
}
