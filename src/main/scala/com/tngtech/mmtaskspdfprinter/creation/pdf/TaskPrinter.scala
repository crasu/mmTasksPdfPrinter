package com.tngtech.mmtaskspdfprinter.creation.pdf

import scala.collection.mutable.ListBuffer

import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._
import com.tngtech.mmtaskspdfprinter.scrum._

private object TaskPrinter {
}

private class TaskPrinter(contentSize: Rectangle, config: Configuration) {

  private val (columnSize, rowSize) = if (config.largeSize) (2,3) else (3, 4)
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

  def create(stories: List[Story]): Seq[PdfPTable] = {
    val pages = ListBuffer[PdfPTable]()
    val storyTaskPairs = for (s <- stories; t <- s.tasks) yield (s, t)
    storyTaskPairs.zipWithIndex foreach {case ((story, task), index) =>
        addTask(pages, index, story, task)
    }
    fillWithEmptyCells(pages, stories.map(_.tasks.size).sum)
    pages.toList
  }

  private def addTask(pages: ListBuffer[PdfPTable], count: Int,
                      story: Story, task: Task) {
    if (count % noOfElementsPerPage == 0) {
      addNewPage(pages)
    }

    val cell = createCell(story, task)
    pages.last.addCell(cell)
  }

  private def addNewPage(pages: ListBuffer[PdfPTable]) {
    val page = new PdfPTable(columnSize)
    page.setWidthPercentage(100)
    pages.append(page)
  }

  private def createCell(story: Story, task: Task): PdfPCell = {
    val cell = createFramingCell()
    var content = taskToPhrase(story, task)
    createInnerTable(cell, content)
    cell
  }

  private def createFramingCell(): PdfPCell = {
    val cell = new PdfPCell()
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(contentSize.getHeight() / rowSize - 1)
    cell
  }

  private def taskToPhrase(story: Story, task: Task): Phrase = {
    val phrase = if (config.largeSize) new Phrase(24) else new Phrase()
    phrase.add(new Chunk(story.name + "\n", config.normalFont))
    phrase.add(new Chunk(task.category + "\n", config.smallFont))
    phrase.add(new Chunk(task.description + "\n", config.bigFont))

    task.subtasks.take(maxNoOfSubtasks - 1).foreach {subtask =>
      phrase.add(square)
      phrase.add(new Chunk(" " + subtask.description + "\n", config.normalFont))
    }
    val remainingTasks = task.subtasks.drop(maxNoOfSubtasks - 1)
    if (!remainingTasks.isEmpty) {
      val descriptions = remainingTasks.map(_.description)
      val concated = descriptions.mkString(", ")
      phrase.add(square)
      phrase.add(new Chunk(" " + concated + "\n", config.normalFont))
    }
    phrase
  }

  private def createInnerTable(outerCell: PdfPCell, phrase: Phrase) {
    var table = new PdfPTable(1)
    table.setWidthPercentage(100.0f)
    var cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(if(config.largeSize) 5.0f else 16.0f)
    cell.setIndent(0)
    cell.addElement(phrase)
    //cell.setFixedHeight(outerCell.getFixedHeight() - 3)
    cell.setFixedHeight(outerCell.getFixedHeight() - config.companyLogo.getScaledHeight - 3)
    table.addCell(cell)

    cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setPaddingLeft(if(config.largeSize) 5.0f else 12.0f)
    cell.setIndent(0)
    cell.addElement(config.companyLogo)
    cell.setFixedHeight(config.companyLogo.getScaledHeight)
    table.addCell(cell)

    outerCell.addElement(table)
  }

  private def fillWithEmptyCells(pages: ListBuffer[PdfPTable], noOfTasksAdded: Int) {
    val emptyTask = Task("", "")
    val emptyStory = Story("", None, None)
    val tasksForFullPage = (
        (noOfTasksAdded.toDouble / noOfElementsPerPage).ceil * noOfElementsPerPage
      ).toInt
    (noOfTasksAdded until tasksForFullPage) foreach { index =>
      addTask(pages, index, emptyStory, emptyTask)
    }
  }
}
