package com.tngtech.mmtaskspdfprinter.creation.pdf.config

import java.util.Properties
import java.io.{File, FileInputStream, BufferedInputStream}
import com.itextpdf.text.pdf._
import com.itextpdf.text.{List => _, _}
import com.tngtech.mmtaskspdfprinter.model.CentralConfiguration

object PdfConfiguration {
  private val LOGO_BASE_NAME = "logo"
  private val IMAGE_SUFFIXES = List(".png", ".gif", ".jpg", ".PNG", ".GIF", ".JPG")

  val defaultConfig: PdfConfiguration = new CentralConfiguration with PdfConfiguration
}

trait PdfConfiguration {
  self: {def properties: Properties} =>

  val hidePriority = properties.getProperty("pdf.hidePriority", "false").toBoolean
  val colour = properties.getProperty("pdf.colour", "44 106 168").split(" ").map(c => c.toInt)
  val largeSize = properties.getProperty("pdf.largeLayout", "false").toBoolean
  val pageSize = PageSize.A4

  val (smallFont, normalFont, bigFont, hugeFont) = {
    /*
    * Vera.ttf is provided by http://www.gnome.org/fonts/
    */
    val baseFontNormal = BaseFont.createFont("/Vera.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    if (largeSize)
      (new Font(baseFontNormal, 12, Font.ITALIC),
       new Font(baseFontNormal, 18, Font.NORMAL),
       new Font(baseFontNormal, 24, Font.BOLD),
       new Font(baseFontNormal, 32, Font.BOLD))
    else
      (new Font(baseFontNormal, 9, Font.ITALIC),
       new Font(baseFontNormal, 10, Font.NORMAL),
       new Font(baseFontNormal, 12, Font.BOLD),
       new Font(baseFontNormal, 16, Font.BOLD))
  }
  

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
