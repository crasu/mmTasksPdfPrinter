package com.tngtech.mmtaskspdfprinter.snippet

import net.liftweb._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.mapper._
import net.liftweb.http.S._
import net.liftweb.http.SHtml._

import net.liftweb.util._
import net.liftweb.util.Helpers._

import scala.xml.{NodeSeq, Text, Group, XML}
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf._
import com.tngtech.mmtaskspdfprinter.pdf.config._
import java.io.ByteArrayOutputStream
import com.itextpdf.text.PageSize

import java.io._

object TaskCreator {
  object selectedBacklogs extends RequestVar[Seq[SprintBacklog]](List())

  val HTML_OK = 200

  def reply() = {
    val pdfBytes = new ByteArrayOutputStream()
    val printer = new PdfPrinter(pdfBytes, new Configuration)
    selectedBacklogs.is.foreach(backlog => printer.addSprintBacklog(backlog))
    printer.toFile()
    Full(InMemoryResponse(pdfBytes.toByteArray(),
                          List("Content-Type" -> "application/pdf"),
                          Nil,
                          HTML_OK))
  }
}
