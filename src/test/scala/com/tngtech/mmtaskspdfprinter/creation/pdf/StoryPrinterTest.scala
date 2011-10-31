package com.tngtech.mmtaskspdfprinter.creation.pdf

import org.specs2.mutable._
import com.tngtech.mmtaskspdfprinter.creation.pdf.config.PdfConfiguration

import com.tngtech.mmtaskspdfprinter.scrum._
import com.itextpdf.text.{ List => ITextList, _ }
import com.itextpdf.text.pdf._

class StoryPrinterTest extends Specification {
  "The StoryPrinter" should {
    var storyCards = 0
    var acceptanceCards = 0
    val printer = new StoryPrinter(new Rectangle(100, 100),
      PdfConfiguration.defaultConfig) {
      override def createStoryCard(story: Story) =
        { storyCards += 1; new PdfPCell() }
      override def createAcceptanceCard(story: Story) =
        { acceptanceCards += 1; new PdfPCell() }
    }
    val s = List(Story("", null, None, Nil),
        Story("", null, None, Nil),
        Story("", null, None, Nil, List("", "", "")))
    printer.create(s)
    "create the correct number of story cards" in {
      // Add one for the empty card.
      storyCards must beEqualTo(3 + 1)
    }
    "create the correct number of acceptance cards" in {
      acceptanceCards must beEqualTo(1)
    }
  }
}