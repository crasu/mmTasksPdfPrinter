package com.tngtech.mmtaskspdfprinter.snippet

import scala.xml.{NodeSeq, Group, XML}
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.common.{Box, Full, Empty}

import net.liftweb.http.js.JE.JsFunc

import com.tngtech.mmtaskspdfprinter.parsing._
import com.tngtech.mmtaskspdfprinter.scrum._

/**
 * Step 2: Backlog selection
 */
trait Selection {
  self: BacklogUpload =>

  def select(xhtml: Group): NodeSeq =
    if (uploadContainer.set_?) {
      askForSprint(xhtml)
    } else {
      NodeSeq.Empty
    }

  private def askForSprint(xhtml: Group): NodeSeq = try {
    val file = uploadContainer.is.open_!.file
    val xml = XML.loadString(new String(file, "UTF-8"))
    val backlogs = (new MmParser).parse(xml).toList
    bind("storySelection", chooseTemplate("choose", "selection", xhtml),
      "story" -> createStorySelectBox(backlogs))
  } catch {
    case ex: org.xml.sax.SAXParseException => S.error("Invalid XML file: " + ex.getMessage); NodeSeq.Empty
    case ex: ParsingException => S.error("Invalid mindmap: " + ex.getMessage); NodeSeq.Empty
  }

  private def createStorySelectBox(allBacklogs: List[Sprint]): NodeSeq = {
    val nonEmptyBacklogs = allBacklogs filter {b => !b.stories.isEmpty}
    val options = nonEmptyBacklogs map {backlog => (backlog.name, backlog.name)}
    val default = if (selectedBacklog.set_? && !selectedBacklog.is.isEmpty) {
      val backlog = selectedBacklog.is.open_!
      backlog.name
    } else {
      ""
    }
    SHtml.ajaxSelect(("", "") :: options,
       Full(default), {selected: String =>
          allBacklogs find (_.name == selected) match {
            case Some(backlog) => selectedBacklog(Full(backlog))
            case None => selectedBacklog(Empty)
          }
          JsFunc("""document.forms["select"].submit""").cmd
       }, "id" -> "sprintSelect")
  }
}
