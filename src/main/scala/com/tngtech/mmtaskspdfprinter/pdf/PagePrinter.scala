package com.tngtech.mmtaskspdfprinter.pdf

import com.itextpdf.text._
import com.itextpdf.text.pdf._
import com.tngtech.mmtaskspdfprinter.scrum._
import scala.List

object PagePrinter {
  val (smallFont, normalFont, bigFont, hugeFont) = {
    /*
    * Vera.ttf is provided by http://www.gnome.org/fonts/
    */
    val baseFontNormal = BaseFont.createFont("/Vera.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    (new Font(baseFontNormal, 9, Font.ITALIC),
     new Font(baseFontNormal, 10),
     new Font(baseFontNormal, 12, Font.BOLD),
     new Font(baseFontNormal, 16, Font.BOLD))
  }
  val companyLogo =  {
    val img = Image.getInstance(getClass().getResource("/TNGLogo.png"))
    img.scalePercent(31.0f)
    img
  }
  val companyBanner = {
    val img = Image.getInstance(getClass().getResource("/TNGLogo.png"))
    img.scalePercent(70.0f)
    img
  }
}

abstract class PagePrinter(val contentSize: Rectangle) {
  protected var _noOfElements = 0
  def noOfElements = _noOfElements
  protected def noOfElements_= (value: Int) {_noOfElements = value}

  protected var pages = List[PdfPTable]()

  def addStory(story: Story)

  def printPages(): List[PdfPTable]
}
