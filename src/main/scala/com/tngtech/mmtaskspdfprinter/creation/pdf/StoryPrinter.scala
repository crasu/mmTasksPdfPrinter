package com.tngtech.mmtaskspdfprinter.creation.pdf

import scala.collection.mutable.ListBuffer

import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._

private object StoryPrinter {
  private val rowSize = 4
}

private class StoryPrinter(val contentSize: Rectangle, val config: Configuration) {

  def create(stories: List[Story]): Seq[PdfPTable] = {
    val pages = ListBuffer[PdfPTable]()
    stories.zipWithIndex foreach {case (story, index) => addStory(pages, index, story)}
    fillWithEmptyCells(pages, stories.size)
    pages.toList
  }
  
  private def addStory(pages: ListBuffer[PdfPTable], count: Int, story: Story) {
    if (count % StoryPrinter.rowSize == 0) {
      addNewPage(pages)
    }

    val storyRow = createStoryRow(story)
    pages.last.addCell(storyRow)
  }

  private def addNewPage(pages: ListBuffer[PdfPTable]) {
    val page = new PdfPTable(1)
    page.setWidthPercentage(100.0f)
    pages.append(page)
  }

  private def createStoryRow(story: Story): PdfPCell = {
    val innerTable = new PdfPTable(2)
    innerTable.setWidthPercentage(100.0f)
    innerTable.setWidths(Array(75, 25))
    val sepHeight = 5
    val footer = createFooter()
    innerTable.addCell(createSeparator(sepHeight))
    val contentHeight = contentSize.getHeight() / StoryPrinter.rowSize -
                        footer.getFixedHeight - sepHeight * 2
    innerTable.addCell(createStoryCell(contentHeight, story))
    innerTable.addCell(createMetaCell(contentHeight, story))
    innerTable.addCell(footer)
    innerTable.addCell(createSeparator(sepHeight))

    new PdfPCell(innerTable)
  }

  private def createSeparator(height: Int): PdfPCell = {
    val cell = new PdfPCell(config.companyBanner, false)
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(height)
    cell.setColspan(2)
    cell.setBackgroundColor(new BaseColor(config.colour(0), config.colour(1), config.colour(2)))
    cell
  }

  private def createFooter(): PdfPCell = {
    val cell = new PdfPCell(config.companyBanner, false)
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(1)
    cell.setPaddingLeft(5)
    cell.setIndent(0)
    cell.setFixedHeight(config.companyBanner.getScaledHeight +
                        cell.getPaddingTop + cell.getPaddingBottom)
    cell.setColspan(2)
    cell
  }

  private def createStoryCell(height: Float, story: Story): PdfPCell = {
    val storyPhrase = new Phrase()
    storyPhrase.add(new Chunk("\n" + story.name, config.hugeFont))
    val storyCell = new PdfPCell(storyPhrase)
    storyCell.setBorder(Rectangle.NO_BORDER)
    storyCell.setPadding(5)
    storyCell.setPaddingRight(30)
    storyCell.setIndent(0)
    storyCell.setFixedHeight(height)
    storyCell
  }

  private def createMetaCell(height: Float, story: Story): PdfPCell = {
    val metaPhrase = new Phrase()
    val undefined =  "________"
    val priority = story.priority.getOrElse(undefined).toString
    val points = story.scrumPoints.getOrElse(undefined).toString
    if (config.hidePriority) {
      metaPhrase.add(new Chunk("\nPriority:  "+priority+"\n\n",
                                config.bigFont))
    } else {
      metaPhrase.add(new Chunk("\n\n\n", config.bigFont))
    }
    metaPhrase.add(new Chunk("\nPoints:    " + points + "\n\n",
                              config.bigFont))
    metaPhrase.add(new Chunk("\nOpened:  "+undefined+"\n\n",
                              config.bigFont))
    metaPhrase.add(new Chunk("\nFinished: "+undefined,
                              config.bigFont))
    val metaCell = new PdfPCell(metaPhrase)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell.setPadding(0)
    metaCell.setPaddingTop(5)
    metaCell.setIndent(0)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell
  }

  private def fillWithEmptyCells(pages: ListBuffer[PdfPTable], noOfStoriesAdded: Int) {
    val emptyStory = Story("", None, None)
    val storiesForFullPage = (
        (noOfStoriesAdded.toDouble / StoryPrinter.rowSize).ceil * StoryPrinter.rowSize
      ).toInt
   (noOfStoriesAdded until storiesForFullPage) foreach { index =>
     addStory(pages, index, emptyStory)
   }
  }
}
