package com.tngtech.mmtaskspdfprinter.pdf

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester
import java.io.ByteArrayOutputStream
import com.itextpdf.text.PageSize

import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf.config._

@RunWith(classOf[JUnitRunner])
class PdfPrinterTest extends Spec with MustMatchers {
    describe("PdfPrinter") {
      val pdfBytes = new ByteArrayOutputStream()
      val printer = new PdfPrinter(pdfBytes, new Configuration())
      val backlog = SprintBacklog("2010-21",
        Story("Some Story: A tale about...", None, None,
          Task("buy Mindstorms set", "Dev"),
          Task("write remote control perl script", "Dev",
            Subtask("write module mod1"),
            Subtask("write module mod2 part a"),
            Subtask("write module mod2 part b")
          ),
          Task("install replacement firmware", "Dev"),
          Task("CT", "regression"),
          Task("Deployment", "deploy to production")
        )
      )
      it("must add tasks to the pdf") {
        printer.addSprintBacklog(backlog)
        printer.numberOfStories must be (1)
      }

      it("and it must write to a file") {
        printer.toFile
        pdfBytes.size must not be(0)
      }
  }

  describe("An empty PdfPrinter") {
    val pdfBytes = new ByteArrayOutputStream()
    val printer = new PdfPrinter(pdfBytes, new Configuration())
    it("must complain about saving an empty file") {
      evaluating  { printer.toFile } must produce [PrinterException]
    }
   }
}