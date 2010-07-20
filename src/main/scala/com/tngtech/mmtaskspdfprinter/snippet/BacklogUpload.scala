package com.tngtech.mmtaskspdfprinter.snippet;

import net.liftweb._
import net.liftweb.http._
import net.liftweb.mapper._
import net.liftweb.http.S._
import net.liftweb.http.SHtml._

import net.liftweb.util._
import net.liftweb.util.Helpers._

import scala.xml.{NodeSeq, Text, Group, XML}
import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.parsing._
import com.tngtech.mmtaskspdfprinter.pdf._
import java.io.ByteArrayOutputStream
import com.itextpdf.text.PageSize

import java.io._

class BacklogUpload {
  private object uploadContainer extends RequestVar[Box[FileParamHolder]](Empty)

  def upload(xhtml: Group) = {
    if (S.get_?) {
      askForFileToUpload(xhtml) // First step
    }
    else {
      askForSprint(xhtml) // Second step
    }
  }

  private def askForFileToUpload(xhtml: Group) = {
    bind("upload", chooseTemplate("choose", "get", xhtml),
                      "fileUpload" -> fileUpload(ul => uploadContainer(Full(ul))))
  }

  private def askForSprint(xhtml: Group) = {
    var backlogs = List[SprintBacklog]()
       uploadContainer.is.foreach(someFile => {
        val xml = XML.loadString(new String(someFile.file, "UTF-8"))
        backlogs ++= MmParser.parse(xml)
      })

      bind("storySelection", chooseTemplate("choose", "post", xhtml),
               "stories" -> extractStorySelection(backlogs))
  }

  private def extractStorySelection(allBacklogs: List[SprintBacklog]) = {
    val nonEmptyBacklogs = allBacklogs filter(!_.stories.isEmpty)
    SHtml.select(nonEmptyBacklogs map (backlog => (backlog.name, backlog.name)),
                   Empty, submit(allBacklogs, _))
  }

  private def submit(allBacklogs: List[SprintBacklog], selectedName: String) {
    val selectedBacklogs = allBacklogs filter (_.name == selectedName)
    S.redirectTo("your_tasks.pdf",
                 () => TaskCreator.selectedBacklogs.set(selectedBacklogs))
  }
}
