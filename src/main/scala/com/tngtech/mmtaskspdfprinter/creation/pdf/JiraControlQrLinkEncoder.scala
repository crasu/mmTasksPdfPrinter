package com.tngtech.mmtaskspdfprinter.creation.pdf

import com.itextpdf.text.Image
import com.itextpdf.text.pdf._
import com.itextpdf.text.pdf.qrcode.EncodeHintType
import com.itextpdf.text.pdf.qrcode.ErrorCorrectionLevel
import java.util.HashMap
import com.itextpdf.text.Chunk

case class QrCodeRenderingInfo(size: Int, xPos: Int, yPos: Int)

object JiraControlQrLinkEncoder {
  def getQrCodeAsChunkFromString(content: String, renderingInfo: QrCodeRenderingInfo) = {
    new Chunk(getQrCodeFromString(content, renderingInfo.size), renderingInfo.xPos, renderingInfo.yPos)
  }
  
  def getQrCodeFromString(content: String, size: Int) = {
    val hints = new HashMap[EncodeHintType,Object]()
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L)
    val barcodeQrCode = new BarcodeQRCode(reencodeStringToUTF8(content), size, size, hints)
    val image  = barcodeQrCode.createAwtImage(java.awt.Color.BLACK, java.awt.Color.WHITE)
    val qrCode = Image.getInstance(image, java.awt.Color.WHITE)
    qrCode
  }

  def getJiraControlUrl(jiraControlUrl : String, projectId : String, jiraKey: String) : String = if (jiraKey.equals("")) {
    ""
  } else {
    val taskId = extractJiraTaskId(jiraKey)
    jiraControlUrl + projectId + '/' + taskId
  }

  def extractJiraTaskId(jiraKey : String) : String = {
    val taskKey = jiraKey.split("-")
    taskKey(taskKey.length - 1)
  }

  def reencodeStringToUTF8(toReencode: String) : String = {
    val reencodedString = new String(toReencode.getBytes("UTF-8"), "UTF-8")
    reencodedString
  }
}
