package com.tngtech.mmtaskspdfprinter

import com.tngtech.mmtaskspdfprinter.parsing._
import scala.xml._
import com.tngtech.mmtaskspdfprinter.scrum._
import joptsimple.OptionParser
import joptsimple.OptionSet
import com.tngtech.mmtaskspdfprinter.creation.pdf.PdfPrinter
import java.io.File
import java.io.FileOutputStream
import com.tngtech.mmtaskspdfprinter.creation.pdf.config.PdfConfiguration

object Main {
  def main(args: Array[String]): Unit = {
    val parser = new OptionParser()
    def fail(msg: String) {
      println("Error: " + msg)
      System.exit(1)
    }
    def failWithHelp(msg: String) {
      println("Error: " + msg)
      println()
      parser.printHelpOn(System.out)
      System.exit(1)
    }

    val fileOpt = parser.accepts("file").
      withRequiredArg().
      required().
      ofType(classOf[File]).
      describedAs("The mindmap");
    parser.accepts("list");
    val nameOpt = parser.accepts("sprint").
      withRequiredArg().
      describedAs("Which sprint to use.").
      ofType(classOf[String])
      
    val pdfOpt = parser.accepts("pdf").
      withRequiredArg().
      describedAs("Where to put the pdf.").
      ofType(classOf[File])

    val options = parser.parse(args: _*);

    val sprints = MmParser.parse(XML.loadFile(fileOpt.value(options)))
    if (options.has("list")) {
      sprints map (_.name) foreach println
    } else {
      val sprint = {
        if (!options.has("sprint"))
          failWithHelp("Argument --sprint missing")
        val name = nameOpt.value(options)
        val sprints2 = sprints filter (_.name == name)
        if (sprints2.isEmpty)
          fail("There is no sprint with name \"" + name + "\"")
        if (sprints2.size > 1)
          fail("There are multiple sprints with name \"" + name + "\"")
        sprints2.head
      }
      val printer = new PdfPrinter(
          () => new FileOutputStream(pdfOpt.value(options)),
          PdfConfiguration.defaultConfig)
      printer.create(List(sprint))
    }
  }
}