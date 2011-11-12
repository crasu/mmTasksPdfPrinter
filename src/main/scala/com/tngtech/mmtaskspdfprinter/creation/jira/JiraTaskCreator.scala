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

  def create(backlogs: List[Sprint]): List[Sprint] = {
    try {
      createBacklog(backlogs)
    } catch {
      case ex: Exception => 
      ex.printStackTrace()
      throw new JiraException("An error occured while sending Data to JIRA", ex)
    }
  }
  
  private def createBacklog(backlogs: List[Sprint]) = {
    val projectId = rpcClient.findProjectId(projectName)
    val backlogsWithKeys = for (backlog <- backlogs) yield {
      val storiesWithKeys = for (story <- backlog.stories) yield {
      	createStory(projectId, story)
      }
      backlog.copy(stories = storiesWithKeys)
    }
    backlogsWithKeys          
  }
  
  private def createStory(projectId: String, story: Story): Story = {
    val (parentId, issueKey) = createIssue(story)
    val tasksWithKeys = for (task <- story.tasks) yield {
      val subissueKey = createSubissue(projectId, parentId, story, task)
      task.copy(jiraKey = subissueKey) 
    }
    story.copy(jiraKey = issueKey, tasks = tasksWithKeys)
  }

  private def createIssue(story: Story) = {
    val issuetype = rpcClient.findIssuetype(config.issuetypename)
    val issue = rpcClient.createIssue(projectName, story.name, issuetype)
    story.copy(jiraKey = issue.key)
    (issue.id, issue.key)
  }

  private def createSubissue(projectId:String, parentId: String, story: Story, task: Task): String = {
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
