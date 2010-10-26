package com.tngtech.mmtaskspdfprinter.pdf

import scala.io.Source
import com.itextpdf.text.pdf._
import com.itextpdf.text._
import scala.List

import java.io.File

object Configuration {
  private val CONFIG_FILE_NAME = "layout.conf"
  private val LOGO_BASE_NAME = "logo"
  private val IMAGE_SUFFIXES = List(".png", ".gif", ".jpg", ".PNG", ".GIF", ".JPG")
}

class Configuration {
  private var properties = {
    var lines = List[String]()
    val file = new File(Configuration.CONFIG_FILE_NAME)
    if (file.exists) {
      lines = Source.
                  fromFile(file).getLines.toList
    }

    val keyValuePair = """^\s*([^=]+)\s*=\s*([^=]+)\s*(#.*)?$""".r
    val empty = """^\s*(#.*)?$""".r
    Map() ++ lines.flatMap( line => {
        line match {
          case keyValuePair(key, value, null) => List((key.trim -> value.trim))
          case keyValuePair(key, value, comment) => List((key.trim -> value.trim))
          case empty(null) => List()
          case empty(comment) => List()
          case invalid => throw new ConfigException("Invalid config line: " + invalid)
        }
      })
  }
  
  private def getAndRemove(key: String, default: String) = {
    val value = properties.getOrElse(key, default)
    properties = properties - key
    value
  }

  val hidePriority = getAndRemove("hidePriority", "0").toInt
  val colour = getAndRemove("colour", "44 106 168").split(" ").map(c => c.toInt)
  val pageSize = PageSize.A4

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
    val existingSuffix = Configuration.IMAGE_SUFFIXES.find(suffix => {
        val file = new File(Configuration.LOGO_BASE_NAME + suffix)
        file.exists
      })
    if (existingSuffix.isEmpty) {
      Image.getInstance(getClass().getResource("/TNGLogo.png"))
    }
    else {
      val awtImg =
        java.awt.Toolkit.getDefaultToolkit()
          .createImage(Configuration.LOGO_BASE_NAME + existingSuffix.get())
      Image.getInstance(awtImg, null)
    }
  }
}
