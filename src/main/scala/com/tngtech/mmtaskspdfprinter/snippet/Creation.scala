package com.tngtech.mmtaskspdfprinter.snippet

import scala.xml.{NodeSeq, Group, XML}
import java.net.URL
import java.net.MalformedURLException
import net.liftweb.http.{SHtml, S}
import net.liftweb.util.Helpers._
import net.liftweb.common.{Box, Full, Empty}
import net.liftweb.http.RequestVar

import com.tngtech.mmtaskspdfprinter.scrum._
import com.tngtech.mmtaskspdfprinter.creation.pdf._
import com.tngtech.mmtaskspdfprinter.creation.jira._

/**
 * Step 3: Task creation
 */
trait Creation {
  self: BacklogUpload =>
  private object jiraUrl extends RequestVar[Box[String]](Empty)
  private object jiraUser extends RequestVar[Box[String]](Empty)
  private object jiraProject extends RequestVar[Box[String]](Empty)

  def create(xhtml: Group): NodeSeq = {
    var jiraPassword = ""

    if (selectedBacklog.set_? && !selectedBacklog.is.isEmpty) {
      val template = bind("jira", chooseTemplate("choose", "create", xhtml),
          "url" -> SHtml.text(jiraUrl.is.getOrElse(""), url => jiraUrl(Full(url)), "id" -> "jiraUrl"),
          "user" -> SHtml.text(jiraUser.is.getOrElse(""), user => jiraUser(Full(user)), "id" -> "jiraUser"),
          "password" -> SHtml.password("", jiraPassword = _, "id" -> "jiraPass"),
          "project" -> SHtml.text(jiraProject.is.getOrElse(""), proj => jiraProject(Full(proj)), "id" -> "jiraProject"),
          "submit" -> SHtml.submit("Send to JIRA",
            () => sendToJira(selectedBacklog.is.get, 
                             jiraUrl.is.get, jiraUser.is.get, jiraPassword, jiraProject.is.get)))
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

    val validationErrors = (
      validateUrl(url).toList :::
      validateText("User", user).toList :::
      validateText("Password", password).toList :::
      validateText("Project", project).toList
    )

    if (validationErrors.isEmpty) {
     try {
       creator.create(List(selectedBacklog))
     } catch {
       case e: Exception => S.error(e.getMessage)
     }
    }
    else {
      S.error(validationErrors.map {msg => <li>{msg}</li>})
    }
  }

  private def validateUrl(url: String): Option[String] =
    try {
      new URL(url)
      None
    } catch {
      case e: MalformedURLException => Some("Invalid URL. A valid example is: http://localhost:8080")
    }

  private def validateText(field: String, text: String): Option[String] =
    if (text.isEmpty) Some(field+" may not be empty")
    else None
}
