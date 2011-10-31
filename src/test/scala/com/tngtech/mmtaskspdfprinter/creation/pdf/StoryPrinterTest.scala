package com.tngtech.mmtaskspdfprinter.creation.pdf

import org.specs2.mutable._

class StoryPrinterTest extends Specification {
  "The StoryPrinter" should {
    "contain 11 characters" in {
      "Hello world" must have size (11)
    }
    "start with 'Hello'" in {
      "Hello world" must startWith("Hello")
    }
    "end with 'world'" in {
      "Hello world" must endWith("world")
    }
  }
}