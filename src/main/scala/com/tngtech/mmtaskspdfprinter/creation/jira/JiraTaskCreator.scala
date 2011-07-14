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
  val rpcClient: RpcClient, val restClient: RestClient,
  val projectName: String) {

  def create(backlogs: List[SprintBacklog]) {
    try {
      val projectId = rpcClient.findProjectId(projectName)
      for (backlog <- backlogs; story <- backlog.stories) {
        val parentId = createIssue(story)
        for (task <- story.tasks) {
          val subissueKey = createSubissue(projectId, parentId, story, task)
          task.jiraKey = subissueKey
        }
      }
      rpcClient.close()
    } catch {
      case ex: Exception => 
      ex.printStackTrace()
      throw new JiraException("An error occured while sending Data to JIRA", ex)
    }
  }

  def createIssue(story: Story) = {
    val issuetype = rpcClient.findIssuetype(config.issuetypename)
    val issue = rpcClient.createIssue(projectName, story.name, issuetype)
    story.jiraKey = issue.key
    issue.id
  }

  def createSubissue(projectId:String, parentId: String, story: Story, task: Task): String = {

    val category =
      if (task.category.isEmpty) ""
      else "Category: " + task.category
      
    val issuetype = rpcClient.findSubissuetype(config.subissuetypename) 

    restClient.createSubissue(projectId, parentId,
      summary = task.description,
      description = task.description + " (" + category + ")\n" +
        task.subtasks.map("- " + _.description).mkString("\n"),
        issuetype)
  }
}
