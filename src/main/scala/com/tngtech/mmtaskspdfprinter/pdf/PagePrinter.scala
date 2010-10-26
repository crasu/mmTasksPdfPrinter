package com.tngtech.mmtaskspdfprinter.pdf

import com.itextpdf.text._
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf.config._
import scala.List

abstract class PagePrinter(val contentSize: Rectangle, val config: Configuration) {
  protected var _noOfElements = 0
  def noOfElements = _noOfElements
  protected def noOfElements_= (value: Int) {_noOfElements = value}

  protected var pages = List[PdfPTable]()
  
  def addStory(story: Story)

  def printPages(): List[PdfPTable]
}
