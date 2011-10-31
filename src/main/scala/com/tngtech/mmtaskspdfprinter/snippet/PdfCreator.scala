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
import com.tngtech.mmtaskspdfprinter.creation.pdf._
import java.io.ByteArrayOutputStream

import java.io._

object PdfCreator {
  object selectedBacklogs extends RequestVar[Seq[SprintBacklog]](List())

  val HTML_OK = 200

  def create() = {
    val printer = new PdfPrinter()
    val backlogs = selectedBacklogs.is.toList
    val pdf = printer.create(backlogs)
    Full(InMemoryResponse(pdf,
                          List("Content-Type" -> "application/pdf"),
                          Nil,
                          HTML_OK))
  }
}
