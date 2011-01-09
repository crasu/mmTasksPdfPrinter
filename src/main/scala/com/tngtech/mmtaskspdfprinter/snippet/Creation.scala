package com.tngtech.mmtaskspdfprinter.snippet

import scala.xml.{NodeSeq, Group, XML}
import net.liftweb.http.{SHtml, S}
import net.liftweb.util.Helpers._
import net.liftweb.common.{Box, Full, Empty}

import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.pdf._
import com.tngtech.mmtaskspdfprinter.creation.jira._

/**
 * Step 3: Task creation
 */
trait Creation {
  self: BacklogUpload =>

  def create(xhtml: Group): NodeSeq = {
    var jiraUrl, jiraUser, jiraPassword, jiraProject = ""

    if (selectedBacklog.set_? && !selectedBacklog.is.isEmpty) {
      val template = bind("jira", chooseTemplate("choose", "create", xhtml),
          "url" -> SHtml.text("", jiraUrl = _),
          "user" -> SHtml.text("", jiraUser = _),
          "password" -> SHtml.password("", jiraPassword = _),
          "project" -> SHtml.text("", jiraProject = _),
          "submit" -> SHtml.submit("Send to JIRA",
            () => sendToJira(selectedBacklog.is.get, 
                             jiraUrl, jiraUser, jiraPassword, jiraProject)))
      bind("pdf", template, "submit" -> SHtml.submit("Create PDF",
            () => createPdf(selectedBacklog.is.get)))
    } else {
      NodeSeq.Empty
    }
  }

  private def createPdf(selectedBacklog: SprintBacklog) {
    S.redirectTo("your_tasks.pdf",
                 () => PdfCreator.selectedBacklogs.set(List(selectedBacklog)))
  }

  private def sendToJira(selectedBacklog: SprintBacklog, url: String,
                         user: String, password: String, project: String) {
    val creator = new JiraTaskCreator(url, user, password, project)
    creator.create(List(selectedBacklog))
  }
}
