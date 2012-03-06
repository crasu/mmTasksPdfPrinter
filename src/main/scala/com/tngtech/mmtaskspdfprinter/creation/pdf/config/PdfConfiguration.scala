package com.tngtech.mmtaskspdfprinter.creation.pdf.config

import java.util.Properties
import java.io.{File, FileInputStream, BufferedInputStream}
import com.itextpdf.text.pdf._
import com.itextpdf.text.{List => _, _}
import com.tngtech.mmtaskspdfprinter.model.CentralConfiguration

object PdfConfiguration {
  private val LOGO_BASE_NAME = "logo"
  private val IMAGE_SUFFIXES = List(".png", ".gif", ".jpg", ".PNG", ".GIF", ".JPG")

  val defaultConfig = new PdfConfiguration
}

class PdfConfiguration extends CentralConfiguration {
    
  val hidePriority = properties.getProperty("pdf.hidePriority", "false").toBoolean
  val colour = properties.getProperty("pdf.colour", "44 106 168").split(" ").map(c => c.toInt)
  val size = SizeType.parse(properties.getProperty("pdf.layout", "medium"))
  val pageSize = PageSize.A4
  val jiraControlUrl = properties.getProperty("pdf.jiraControlUrl")
  val jiraControlProjectId : Int = properties.getProperty("pdf.jiraControlProjectId", "0").toInt

  def generateQrCodes() = (!jiraControlUrl.equals("") && (jiraControlProjectId != 0))

  val companyLogo =  {
    val img = fetch_logo()
    img.scaleToFit(93, 46)
    img
  }
  val companyBanner = {
    val img = fetch_logo()
    img.scaleToFit(210, 50)
    img
  }

  private def fetch_logo() = {
    val existingSuffix = PdfConfiguration.IMAGE_SUFFIXES.find(suffix => {
        val file = new File(PdfConfiguration.LOGO_BASE_NAME + suffix)
        file.exists
      })
    if (existingSuffix.isEmpty) {
      Image.getInstance(getClass().getResource("/TNGLogo.png"))
    }
    else {
      val awtImg =
        java.awt.Toolkit.getDefaultToolkit()
          .createImage(PdfConfiguration.LOGO_BASE_NAME + existingSuffix.get)
      Image.getInstance(awtImg, null)
    }
  }
}


