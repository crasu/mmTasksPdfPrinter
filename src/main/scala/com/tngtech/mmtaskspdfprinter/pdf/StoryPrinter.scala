package com.tngtech.mmtaskspdfprinter.pdf

import com.itextpdf.text._
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import scala.List

object StoryPrinter {
  private val rowSize = 4
}

class StoryPrinter(contentSize: Rectangle) extends PagePrinter(contentSize) {

  override def addStory(story: Story) {
    if (noOfElements % StoryPrinter.rowSize == 0) {
      addNewPage()
    }

    val storyRow = createStoryRow(story)
    pages.last.addCell(storyRow)

    noOfElements += 1
  }

  private def addNewPage() {
    val page = new PdfPTable(1)
    page.setWidthPercentage(100.0f)
    pages += page
  }

  private def createStoryRow(story: Story) = {
    val innerTable = new PdfPTable(2)
    innerTable.setWidthPercentage(100.0f)
    innerTable.setWidths(Array(75, 25))
    val sepHeight = 5
    val footer = createFooter()
    innerTable.addCell(createSeperator(sepHeight))
    val contentHeight = contentSize.getHeight() / StoryPrinter.rowSize -
                        footer.getFixedHeight - sepHeight * 2
    innerTable.addCell(createStoryCell(contentHeight, story))
    innerTable.addCell(createMetaCell(contentHeight, story))
    innerTable.addCell(footer)
    innerTable.addCell(createSeperator(sepHeight))

    new PdfPCell(innerTable)
  }

  private def createSeperator(height: Int) = {
    val cell = new PdfPCell(PagePrinter.companyBanner, false)
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(height)
    cell.setColspan(2)
    cell.setBackgroundColor(new BaseColor(44, 106, 168))
    cell
  }

  private def createFooter() = {
    val cell = new PdfPCell(PagePrinter.companyBanner, false)
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(1)
    cell.setPaddingLeft(5)
    cell.setIndent(0)
    cell.setFixedHeight(PagePrinter.companyBanner.getScaledHeight +
                        cell.getPaddingTop + cell.getPaddingBottom)
    cell.setColspan(2)
    cell
  }

  private def createStoryCell(height: Float, story: Story) = {
    val storyPhrase = new Phrase()
    storyPhrase.add(new Chunk("\n" + story.name, PagePrinter.hugeFont))
    val storyCell = new PdfPCell(storyPhrase)
    storyCell.setBorder(Rectangle.NO_BORDER)
    storyCell.setPadding(5)
    storyCell.setIndent(0)
    storyCell.setFixedHeight(height)
    storyCell
  }

  private def createMetaCell(height: Float, story: Story) = {
    val metaPhrase = new Phrase()
    val undefined =  "________"
    val priority = if (story.priority == Story.NO_PRIORITY) undefined
                   else ""+story.priority
    val points = if (story.scrumPoints == Story.NO_ESTIMATION) undefined
                 else ""+story.scrumPoints
    metaPhrase.add(new Chunk("\nPriority:  "+priority+"\n\n",
                              PagePrinter.bigFont))
    metaPhrase.add(new Chunk("\nPoints:    " + points + "\n\n\n",
                              PagePrinter.bigFont))
    metaPhrase.add(new Chunk("\nOpened:  "+undefined+"\n\n",
                              PagePrinter.bigFont))
    metaPhrase.add(new Chunk("\nFinished: "+undefined,
                              PagePrinter.bigFont))
    val metaCell = new PdfPCell(metaPhrase)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell.setPadding(0)
    metaCell.setIndent(0)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell
  }

  override def printPages() = {
    fillWithEmptyCells
    pages
  }

  private def fillWithEmptyCells() {
    val emptyStory = Story("")
    while (noOfElements % StoryPrinter.rowSize != 0) {
      addStory(emptyStory)
    }
  }
}
