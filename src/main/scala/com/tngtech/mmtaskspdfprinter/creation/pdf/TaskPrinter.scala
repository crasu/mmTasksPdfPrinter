package com.tngtech.mmtaskspdfprinter.creation.pdf

import scala.collection.mutable.ListBuffer

import com.itextpdf.text.{List => ITextList, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._
import com.tngtech.mmtaskspdfprinter.scrum._

private object TaskPrinter {
}

private class TaskPrinter(contentSize: Rectangle, config: PdfConfiguration) {

  import config.size._
  private val noOfElementsPerPage = taskColumnNumber * taskRowNumber
  
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
    val page = new PdfPTable(config.size.taskColumnNumber)
    page.setWidthPercentage(100)
    pages.append(page)
  }

  private def createCell(story: Story, task: Task): PdfPCell = {
    val cell = createFramingCell()
    var (titles, subtasklist) = taskToItext(story, task)
    createInnerTable(cell, titles, subtasklist)
    cell
  }

  private def createFramingCell(): PdfPCell = {
    val cell = new PdfPCell()
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(contentSize.getHeight() / taskRowNumber - 1)
    cell
  }

  private def taskToItext(story: Story, task: Task): (Phrase, ITextList) = {
    val titles = new Phrase(config.size.leading)
    titles.add(new Chunk(story.name + "\n", config.size.normalFont))
    titles.add(new Chunk(task.category + "\n", config.size.smallFont))
    titles.add(new Chunk(task.jiraKey, config.size.normalFont))
    titles.add(new Chunk(task.description + "\n", config.size.bigFont))
    val subtasklist = new ITextList(ITextList.UNORDERED)
    task.subtasks.take(maxNoOfSubtasks - 1).foreach {subtask =>
      subtasklist.add(new ListItem(subtask.description, config.size.normalFont))
    }
    val remainingTasks = task.subtasks.drop(maxNoOfSubtasks - 1)
    if (!remainingTasks.isEmpty) {
      val descriptions = remainingTasks.map(_.description)
      val concated = descriptions.mkString(", ")
      subtasklist.add(new ListItem(concated, config.size.normalFont))
    }
    (titles, subtasklist)
  }
  

  private def createInnerTable(outerCell: PdfPCell, titles: Phrase, subtasklist: ITextList) {
    var table = new PdfPTable(config.size.innerTableCols)
    table.setWidthPercentage(100f)
    if (config.size.innerTableCols == 2) table.setWidths(
        Array( (contentSize.getWidth() / taskColumnNumber - 1) -  config.companyLogo.getScaledHeight,
            config.companyLogo.getScaledHeight + 15))
    var cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(config.size.paddingContent)
    cell.setIndent(0)
    cell.addElement(titles)
    cell.addElement(subtasklist)
    cell.setRotation(config.size.cellRotation)
    cell.setFixedHeight(outerCell.getFixedHeight() - config.companyLogo.getScaledHeight - 3)
    table.addCell(cell)

    cell = new PdfPCell()
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setPaddingLeft(config.size.paddingLeft)
    cell.setIndent(0)
    cell.addElement(config.companyLogo)
    cell.setRotation(config.size.cellRotation)
    cell.setFixedHeight(config.companyLogo.getScaledHeight)
    table.addCell(cell)

    outerCell.addElement(table)
  }
  
  private def fillWithEmptyCells(pages: ListBuffer[PdfPTable], noOfTasksAdded: Int) {
    val emptyTask = Task("", "")
    val emptyStory = Story("", UndefScrumPoints, None)
    val tasksForFullPage = (
        (noOfTasksAdded.toDouble / noOfElementsPerPage).ceil * noOfElementsPerPage
      ).toInt
    (noOfTasksAdded until tasksForFullPage) foreach { index =>
      addTask(pages, index, emptyStory, emptyTask)
    }
  }
}
