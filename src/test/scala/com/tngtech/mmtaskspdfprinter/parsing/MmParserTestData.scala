package com.tngtech.mmtaskspdfprinter.parsing

import scala.io.Source

object MmParserTestData {
val validData = {
  val s = Source.fromURL(getClass().getClassLoader().getResource("MM2CSV.t.input.mm"))
  s.getLines().mkString("/n") 
}

val invalidData = """<mm version="0.9.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1265988225850" ID="ID_204900544" MODIFIED="1269505504609" TEXT="Product Backlog">
</node>
</mm>"""
}
