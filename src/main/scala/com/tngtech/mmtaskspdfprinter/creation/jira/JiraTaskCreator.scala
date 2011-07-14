package com.tngtech.mmtaskspdfprinter.creation.jira

import org.apache.xmlrpc._
import org.apache.xmlrpc.client._
import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.methods._
import java.net.URL
import scala.collection.JavaConversions._
import config._

import com.tngtech.mmtaskspdfprinter.scrum._

class JiraException(msg: String, cause: Exception) extends Exception(msg, cause)

object JiraTaskCreator {
  val rpcPath = "/rpc/xmlrpc"
}

class JiraTaskCreator(val config: JiraConfiguration,
                      rawUrl: String, val user: String,
                      val pass: String, val projectName: String) {

  private val url = """\/$""".r.replaceAllIn(rawUrl, "")

  private val rpcClient = try {
    val rpcClient = new XmlRpcClient()
    val config = new XmlRpcClientConfigImpl()
    config.setServerURL(new URL(url+JiraTaskCreator.rpcPath))
    rpcClient.setConfig(config)
    rpcClient
  } catch {
    case ex: org.apache.xmlrpc.XmlRpcException => throw new JiraException("Failed to setup connection to JIRA", ex)
  }

  private val loginToken = try {
    rpcClient.execute("jira1.login", List(user, pass)).toString
  } catch {
    case ex: org.apache.xmlrpc.XmlRpcException => throw new JiraException("Failed to login to JIRA", ex)
  }

  def create(backlogs: List[SprintBacklog]) {
    try {
      val projects = rpcClient.execute("jira1.getProjectsNoSchemes", List(loginToken)).
                      asInstanceOf[Array[AnyRef]].
                        map(_.asInstanceOf[java.util.HashMap[String, String]])
      val project = projects.find(projectName == _.get("key"))
      if (project.isEmpty) {
        rpcClient.execute("jira1.logout", List(loginToken))
        throw new Exception("JIRA project "+projectName+" doesn't exist!\n" +
                            "Please create it or choose one of these projects: "+
                             projects.map(_.get("key")).toList)
      }
      
      val projectId = project.get.get("id")
      for (b <- backlogs; s <- b.stories) {
        val (parentId,parentKey) = createIssue(s)
        s.jiraKey = parentKey
        for (t <- s.tasks) {
          val subissueKey = createSubissue(projectId, parentId, s, t)
          t.jiraKey = subissueKey
        }
      }
      rpcClient.execute("jira1.logout", List(loginToken))
    } catch {
      case ex: Exception => throw new JiraException("An error occured while sending Data to JIRA", ex)
    }
  }

  def createIssue(story: Story): (String,String) = {
      val args: java.util.Map[String, String] =
        Map(
          "project" -> projectName,
          "type" -> "3",
          "summary" -> story.name,
          "description" -> story.name
        )
      val issue = rpcClient.execute("jira1.createIssue", List(loginToken, args)).asInstanceOf[java.util.HashMap[String, Object]]
      return (issue.get("id").toString, issue.get("key").toString)
  }

  def createSubissue(projectId: String, parentId: String, story: Story, task: Task): String = {
    val token = "os_username=" + user + "&os_password=" + pass
    val restClient = new JiraRestClient(url+"/secure/CreateSubTaskIssueDetails.jspa?"+token)
    val category =
        if (task.category.isEmpty) ""
        else "Category: " + task.category + "\n"
     restClient.post(
       Map(),
       Map(
         "summary" -> task.description,
         "priority" -> "3",
         "duedate" -> "",
         "assignee" -> "-1",
         "reporter" -> user,
         "environment" -> "",
         "description" ->
            (task.description+" ("+task.category+")\n" +
             task.subtasks.map("- "+_.description).mkString("\n")),
         "timetracking_originalestimate" -> "",
         "timetracking_remainingestimate" -> "",
         "isCreateIssue" -> "true",
         "hasWorkStarted" -> "",
         "issuetype" -> config.subissueid,
         "viewIssueKey" -> "",
         "pid" -> projectId,
         "parentIssueId" -> parentId,
         "Create" -> "Create"
       )
     )
  }
}
