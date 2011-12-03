package com.tngtech.mmtaskspdfprinter.creation.pdf

import scala.collection.mutable.ListBuffer

import com.itextpdf.text.{ List => ITextList, _ }
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._
import com.tngtech.mmtaskspdfprinter.scrum._

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
    val cards = for (s <- stories; t <- s.tasks)
      yield createCell(s, t)

    val pages = cards grouped noOfElementsPerPage map { cells =>
      val page = new PdfPTable(config.size.taskColumnNumber)
      page.setWidthPercentage(100)
      cells foreach (page.addCell)
      for (_ <- cells.size until noOfElementsPerPage)
        page addCell createFramingCell
      page
    }

    pages.toList
  }

  private def createCell(story: Story, task: Task): PdfPCell = {
    val cell = createFramingCell()
    val titles = {
      val titles = new Phrase(config.size.leading)
      titles.add(new Chunk(story.name + "\n", config.size.normalFont))
      titles.add(new Chunk(task.category + "\n", config.size.smallFont))
      titles.add(new Chunk(task.jiraKey + "\n", config.size.normalFont))
      titles.add(new Chunk(task.description + "\n", config.size.bigFont))
      titles
    }
    var subtasklist = {
      val subtasklist = new ITextList(ITextList.UNORDERED)
      val (t1, t2) = task.subtasks.splitAt(maxNoOfSubtasks - 1)
      
      for (subtask <- t1)
        subtasklist.add(new ListItem(subtask.description, config.size.normalFont))
      
      if (!t2.isEmpty) {
        val descriptions = t2.map(_.description).mkString(", ")
        subtasklist.add(new ListItem(descriptions, config.size.normalFont))
      }
      subtasklist
    }
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

  private def createInnerTable(outerCell: PdfPCell, titles: Phrase, subtasklist: ITextList) {
    var table = new PdfPTable(config.size.innerTableCols)
    table.setWidthPercentage(100f)
    if (config.size.innerTableCols == 2) table.setWidths(
      Array((contentSize.getWidth() / taskColumnNumber - 1) - config.companyLogo.getScaledHeight,
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
}
