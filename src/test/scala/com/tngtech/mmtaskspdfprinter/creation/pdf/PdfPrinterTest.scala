package com.tngtech.mmtaskspdfprinter.creation.pdf

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
      val printer = new PdfPrinter(() => new ByteArrayOutputStream())
      val backlog = SprintBacklog("2010-21",
        Story("Some Story: A tale about...", UndefScrumPoints, None, List(
          Task("buy Mindstorms set", "Dev"),
          Task("write remote control perl script", "Dev",  List(
            Subtask("write module mod1"),
            Subtask("write module mod2 part a"),
            Subtask("write module mod2 part b")
          )),
          Task("install replacement firmware", "Dev"),
          Task("CT", "regression"),
          Task("Deployment", "deploy to production")
        ))
      )

      it("it must write to a file without error") {
        val pdf = printer.create(List(backlog))
        pdf.size must not be(0)
      }
  }
}