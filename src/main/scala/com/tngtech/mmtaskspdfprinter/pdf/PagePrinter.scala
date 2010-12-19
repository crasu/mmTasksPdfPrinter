package com.tngtech.mmtaskspdfprinter.pdf

import com.itextpdf.text.{List => _, _}
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf.config._

abstract class PagePrinter(val contentSize: Rectangle, val config: Configuration) {
  protected var _noOfElements: Int = 0
  def noOfElements = _noOfElements
  protected def noOfElements_= (value: Int) {_noOfElements = value}

  protected var pages: Seq[PdfPTable] = Vector()

  def addStory(story: Story): Unit

  def printPages(): Seq[PdfPTable]
}
