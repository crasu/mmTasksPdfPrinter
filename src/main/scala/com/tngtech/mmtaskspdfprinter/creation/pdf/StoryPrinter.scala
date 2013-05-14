package com.tngtech.mmtaskspdfprinter.creation.pdf

import scala.collection.mutable.ListBuffer

import com.itextpdf.text.{ List => ITextList, _ }
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config._

class StoryPrinter(val contentSize: Rectangle, val config: PdfConfiguration) {

  import config.size.{ rowNumber => rowSize }

  def create(stories: Seq[Story]): Seq[PdfPTable] = {
    val (withoutAcceptance, withAcceptance) = stories span (_.acceptanceCriteria.isEmpty)

    val l1 = withAcceptance flatMap { s => Vector(createStoryCard(s), createAcceptanceCard(s)) }
    val l2 = withoutAcceptance map createStoryCard

    val emptyCell = createStoryCard(Story("", UndefScrumPoints, None, List()))

    val pages = (l1 ++ l2) grouped rowSize map { cells =>
      val page = new PdfPTable(1)
      page.setWidthPercentage(100.0f)
      for (c <- cells)
        page addCell c
      for (_ <- cells.size until rowSize)
        page addCell emptyCell
      page
    }

    pages.toList
  }

  val sepHeight = 5

  private val separator: PdfPCell = {
    val cell = new PdfPCell(config.companyBanner, false)
    cell.setBorder(Rectangle.NO_BORDER)
    cell.setPadding(0)
    cell.setIndent(0)
    cell.setFixedHeight(sepHeight)
    cell.setColspan(2)
    cell.setBackgroundColor(new BaseColor(config.colour(0), config.colour(1), config.colour(2)))
    cell
  }

  private val footer: PdfPCell = {
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

  val contentHeight = contentSize.getHeight() / rowSize -
    footer.getFixedHeight - sepHeight * 2

  def createAcceptanceCard(story: Story): PdfPCell = {
    val innerTable = new PdfPTable(1)
    innerTable.setWidthPercentage(100.0f)

    val list = new ITextList(ITextList.UNORDERED)
    for (s <- story.acceptanceCriteria)
      list.add(new ListItem(s, config.size.normalFont))

    var headerCell = {
      val headerCell = new PdfPCell()
      headerCell.setBorder(Rectangle.NO_BORDER)
      headerCell.setPadding(config.size.paddingContent)
      val headerPhrase = new Phrase()
      headerPhrase.add(new Chunk("\n\nAcceptance Criteria:\n\n", config.size.bigFont))
      headerCell.addElement(headerPhrase)
      headerCell.addElement(list)
      headerCell.setFixedHeight(contentHeight)
      headerCell
    }

    innerTable.addCell(separator)
    innerTable.addCell(headerCell)
    innerTable.addCell(footer)
    innerTable.addCell(separator)

    new PdfPCell(innerTable)
  }

  def createStoryCard(story: Story): PdfPCell = {
    val innerTable = new PdfPTable(2)
    innerTable.setWidthPercentage(100.0f)
    innerTable.setWidths(Array(70, 30))

    val storyCell = {
      val storyPhrase = new Phrase()
      storyPhrase.add(new Chunk("\n" + story.name, config.size.hugeFont))
      storyPhrase.add(new Chunk("\n\n\n" + story.key + "\n", config.size.bigFont))
      if (!story.key.equals("") && config.generateQrCodes()) {
        storyPhrase.add(JiraControlQrLinkEncoder.getQrCodeAsChunkFromString(JiraControlQrLinkEncoder.getJiraControlUrl(config.jiraControlUrl, config.jiraControlProjectId, story.key), config.size.storyQrCodeRenderingInfo))}

      val storyCell = new PdfPCell(storyPhrase)
      storyCell.setBorder(Rectangle.NO_BORDER)
      storyCell.setPadding(5)
      storyCell.setPaddingRight(30)
      storyCell.setIndent(0)
      storyCell.setFixedHeight(contentHeight)
      storyCell
    }

    innerTable.addCell(separator)
    innerTable.addCell(storyCell)
    innerTable.addCell(createMetaCell(story))
    innerTable.addCell(footer)
    innerTable.addCell(separator)

    new PdfPCell(innerTable)
  }

  private def createMetaCell(story: Story): PdfPCell = {
    val metaPhrase = new Phrase()
    val undefined = "________"
    val priority = story.priority.getOrElse(undefined).toString
    val points = story.scrumPoints.toString(undefined)
    if (!config.hidePriority) {
      metaPhrase.add(new Chunk("\nPriority:  " + priority + "\n\n",
        config.size.bigFont))
    } else {
      metaPhrase.add(new Chunk("\n\n\n", config.size.bigFont))
    }
    metaPhrase.add(new Chunk("\nPoints:    " + points + "\n\n",
      config.size.bigFont))
    metaPhrase.add(new Chunk("\nOpened:  " + undefined + "\n\n",
      config.size.bigFont))
    metaPhrase.add(new Chunk("\nFinished: " + undefined,
      config.size.bigFont))
    val metaCell = new PdfPCell(metaPhrase)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell.setPadding(0)
    metaCell.setPaddingTop(5)
    metaCell.setIndent(0)
    metaCell.setBorder(Rectangle.NO_BORDER)
    metaCell
  }
}
