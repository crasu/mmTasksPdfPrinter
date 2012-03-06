package com.tngtech.mmtaskspdfprinter.creation.pdf.config

import com.itextpdf.text.pdf._
import com.itextpdf.text.{ List => _, _ }
import com.tngtech.mmtaskspdfprinter.creation.pdf.QrCodeRenderingInfo

object SizeType {
  def parse(s: String) = s match {
    case "small" => SmallSizeType
    case "medium" => MediumSizeType
    case "large" => LargeSizeType
    case _ => throw new RuntimeException("Invalid size: " + s)
  }
}
trait SizeType {
  def smallFont: Font
  def normalFont: Font
  def bigFont: Font
  def hugeFont: Font
  val baseFontNormal = BaseFont.createFont("/Vera.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

  def rowNumber: Int

  def leading: Float

  def paddingContent: Float
  def paddingLeft: Float

  def taskRowNumber: Int
  def taskColumnNumber: Int

  def maxNoOfSubtasks: Int
  def cellRotation: Int //0 for portrait tasks, 90 for landscape tasks
  
  def innerTableCols: Int //1 for portrait tasks, 2 for landscape tasks

  def storyQrCodeRenderingInfo : QrCodeRenderingInfo
  def tasksQrCodeRenderingInfo : QrCodeRenderingInfo
  def tasksQrCodeHeightOffset : Int
  def hideTasksProjectKey : Boolean
}

case object SmallSizeType extends SizeType {
  val (smallFont, normalFont, bigFont, hugeFont) =
    (new Font(baseFontNormal, 9, Font.ITALIC),
      new Font(baseFontNormal, 10, Font.NORMAL),
      new Font(baseFontNormal, 12, Font.BOLD),
      new Font(baseFontNormal, 16, Font.BOLD))
  val rowNumber = 4
  val leading = 16f
  val paddingContent = 16f
  val paddingLeft = 12f
  val taskRowNumber = 4
  val taskColumnNumber = 3
  val maxNoOfSubtasks = 4
  val cellRotation = 0
  val innerTableCols = 1

  val storyQrCodeRenderingInfo = QrCodeRenderingInfo(75, 100, -55)
  val tasksQrCodeRenderingInfo = QrCodeRenderingInfo(40, 0, -8)
  val tasksQrCodeHeightOffset = 5
  val hideTasksProjectKey = true
}

case object MediumSizeType extends SizeType {
  val (smallFont, normalFont, bigFont, hugeFont) =
    (new Font(baseFontNormal, 12, Font.ITALIC),
      new Font(baseFontNormal, 18, Font.NORMAL),
      new Font(baseFontNormal, 24, Font.BOLD),
      new Font(baseFontNormal, 32, Font.BOLD))
  val rowNumber = 2
  val leading = 24f
  val paddingContent = 5f
  val paddingLeft = 5f
  val taskRowNumber = 2
  val taskColumnNumber = 2
  val maxNoOfSubtasks = 6
  val cellRotation = 0
  val innerTableCols = 1

  val storyQrCodeRenderingInfo = QrCodeRenderingInfo(125, 0, -125)
  val tasksQrCodeRenderingInfo = QrCodeRenderingInfo(90, 0, -20)
  val tasksQrCodeHeightOffset = 10
  val hideTasksProjectKey = true
}

case object LargeSizeType extends SizeType {
  val (smallFont, normalFont, bigFont, hugeFont) =
    (new Font(baseFontNormal, 12, Font.ITALIC),
      new Font(baseFontNormal, 18, Font.NORMAL),
      new Font(baseFontNormal, 24, Font.BOLD),
      new Font(baseFontNormal, 32, Font.BOLD))
  val rowNumber = 2
  val leading = 24f
  val paddingContent = 5f
  val paddingLeft = 2f
  val taskRowNumber = 2
  val taskColumnNumber = 2
  val maxNoOfSubtasks = 5
  val cellRotation = 90
  val innerTableCols = 2

  val storyQrCodeRenderingInfo = QrCodeRenderingInfo(125, 0, -125)
  val tasksQrCodeRenderingInfo = QrCodeRenderingInfo(90, 208, 15)
  val tasksQrCodeHeightOffset = 0
  val hideTasksProjectKey = false
}
