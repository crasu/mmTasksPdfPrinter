package com.tngtech.mmtaskspdfprinter.pdf

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester
import java.io.ByteArrayOutputStream
import com.itextpdf.text.PageSize

import com.tngtech.mmtaskspdfprinter.scrum._

@RunWith(classOf[JUnitRunner])
class PdfPrinterTest extends Spec with MustMatchers {
    describe("PdfPrinter") {
      val pdfBytes = new ByteArrayOutputStream()
      val printer = new PdfPrinter(pdfBytes, PageSize.A4)
      val backlog = SprintBacklog("2010-21")
      backlog.stories += {
        var story = Story("Some Story: A tale about...")
        story.tasks += Task("buy Mindstorms set", "Dev")
        story.tasks += {
          var task = Task("write remote control perl script", "Dev")
          task.subtasks += Subtask("write module mod1")
          task.subtasks += Subtask("write module mod2 part a")
          task.subtasks += Subtask("write module mod2 part b")
          task
        }
        story.tasks += Task("install replacement firmware", "Dev")
        story.tasks += Task("CT", "regression")
        story.tasks += Task("Deployment", "deploy to production")
        story
      }
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
    val printer = new PdfPrinter(pdfBytes, PageSize.A4)
    it("must complain about saving an empty file") {
      evaluating  { printer.toFile } must produce [PrinterException]
    }
   }
}