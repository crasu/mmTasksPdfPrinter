package com.tngtech.mmtaskspdfprinter.creation.jira

import org.apache.xmlrpc._
import org.apache.xmlrpc.client._
import org.apache.commons.httpclient._
import org.apache.commons.httpclient.methods.multipart._
import org.apache.commons.httpclient.methods._
import java.net.URL
import scala.collection.JavaConversions._

import com.tngtech.mmtaskspdfprinter.scrum._

object JiraTaskCreator {
  val rpcPath = "/rpc/xmlrpc"
}

class JiraTaskCreator(rawUrl: String, val user: String,
                      val pass: String, val projectName: String) {

  private val url = """\/$""".r.replaceAllIn(rawUrl, "")

  private val rpcClient = {
    val rpcClient = new XmlRpcClient()
    val config = new XmlRpcClientConfigImpl()
    config.setServerURL(new URL(url+JiraTaskCreator.rpcPath))
    rpcClient.setConfig(config)
    rpcClient
  }

  private val loginToken = rpcClient.execute("jira1.login", List(user, pass)).toString

  def create(backlogs: List[SprintBacklog]) {  
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
      val parentId = createIssue(s)
      for (t <- s.tasks) {
        createSubissue(projectId, parentId, s, t)
      }
    }
    rpcClient.execute("jira1.logout", List(loginToken))
  }

  def createIssue(story: Story): String = {
      val args: java.util.Map[String, String] =
        Map(
          "project" -> projectName,
          "type" -> "3",
          "summary" -> story.name,
          "description" -> story.name
        )
      val issue = rpcClient.execute("jira1.createIssue", List(loginToken, args)).asInstanceOf[java.util.HashMap[String, Object]]
      issue.get("id").toString
  }

  def createSubissue(projectId: String, parentId: String, story: Story, task: Task) {
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
         "issuetype" -> "5",
         "viewIssueKey" -> "",
         "pid" -> projectId,
         "parentIssueId" -> parentId,
         "Create" -> "Create"
       )
     )
  }
}