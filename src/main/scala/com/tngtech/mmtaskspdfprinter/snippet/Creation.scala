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
import com.tngtech.mmtaskspdfprinter.model._
import com.tngtech.mmtaskspdfprinter.creation.jira.config._

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
      val config = new CentralConfiguration with JiraConfiguration
      val template = bind("jira", chooseTemplate("choose", "create", xhtml),
          "url" -> SHtml.text(jiraUrl.is.getOrElse(config.hostname), url => jiraUrl(Full(url)), "id" -> "jiraUrl"),
          "user" -> SHtml.text(jiraUser.is.getOrElse(config.user), user => jiraUser(Full(user)), "id" -> "jiraUser"),
          "password" -> SHtml.password(config.password, jiraPassword = _, "id" -> "jiraPass"),
          "project" -> SHtml.text(jiraProject.is.getOrElse(config.project), proj => jiraProject(Full(proj)), "id" -> "jiraProject"),
          "submit" -> SHtml.submit("Send to JIRA",
            () => {
              val updatedBacklog = sendToJira(config, selectedBacklog.is.get,
                             jiraUrl.is.get, jiraUser.is.get, jiraPassword, jiraProject.is.get)
              if (updatedBacklog.isDefined) selectedBacklog.set(Full(updatedBacklog.get))
            }))
      bind("pdf", template, "submit" -> SHtml.submit("Create PDF",
            () => createPdf(selectedBacklog.is.get)))
    } else {
      NodeSeq.Empty
    }
  }

  private def createPdf(selectedBacklog: Sprint) {
    S.redirectTo("your_tasks.pdf",
                 () => PdfCreator.selectedBacklogs.set(List(selectedBacklog)))
  }

  private def sendToJira(config: JiraConfiguration,
                         selectedBacklog: Sprint, url: String,
                         user: String, password: String, project: String): Option[Sprint] = {
    val validationErrors = (
      validateUrl(url).toList :::
      validateText("User", user).toList :::
      validateText("Password", password).toList :::
      validateText("Project", project).toList
    )

    if (validationErrors.isEmpty) {
     try {
       val sprint = connectAndSendToJira(config, selectedBacklog,
           url, user, password, project)
       S.notice(<li style="color:black">Done</li>)
       sprint
     } catch {
       case e: Exception => {
           LastError(Full(e))
           S.error(<li style="color:crimson"> {e.getMessage.split("\n").head + " "} <a href="error">more ...</a></li>)
       }
       None
     }
    }
    else {
      S.error(validationErrors.map {msg => <li style="color:crimson">{msg}</li>})
      None
    }
  }

private def connectAndSendToJira(config: JiraConfiguration,
                         selectedBacklog: Sprint, url: String,
                         user: String, password: String, project: String): Option[Sprint] = {
   val soapClient = new SoapClient(url, user, password)
   try {
     soapClient.login
  	 val creator = new JiraTaskCreator(config, soapClient, project)
  	 val updatedList = creator.create(List(selectedBacklog))
     updatedList.headOption
   } finally {
     soapClient.logout
   }      
}
  
  private def validateUrl(url: String): Option[String] =
    if (url.isEmpty) Some("URL may not be empty")
    else {
      try {
        new URL(url)
        None
      } catch {
        case e: MalformedURLException => Some("Invalid URL. A valid example is: http://localhost:8080")
      }
    }

  private def validateText(field: String, text: String): Option[String] =
    if (text.isEmpty) Some(field+" may not be empty")
    else None
}
