package com.tngtech.mmtaskspdfprinter.creation.jira

import scala.collection.JavaConversions._
import config._

import com.tngtech.mmtaskspdfprinter.scrum._

class JiraTaskCreator(val config: JiraConfiguration,
  val soapClient: SoapClient,  val projectName: String) {

  def create(backlogs: List[Sprint]): List[Sprint] =
    createBacklog(backlogs)
  
  private def createBacklog(backlogs: List[Sprint]) = {
    val backlogsWithKeys = for (backlog <- backlogs) yield {
      val storiesWithKeys = for (story <- backlog.stories) yield {
      	createStory(projectName, story)
      }
      backlog.copy(stories = storiesWithKeys)
    }
    backlogsWithKeys          
  }
  
  private def createStory(projectId: String, story: Story): Story = {
    val issueKey = createIssue(story)
    val tasksWithKeys = for (task <- story.tasks) yield {
      val subissueKey = createSubissue(projectId, issueKey, story, task)
      task.copy(jiraKey = subissueKey) 
    }
    story.copy(jiraKey = issueKey, tasks = tasksWithKeys)
  }

  private def createIssue(story: Story) = 
    soapClient.createIssue(projectName, story.name, config.issuetypename)

  private def createSubissue(projectId:String, parentId: String, story: Story, task: Task): String = {
    val category =
      if (task.category.isEmpty) ""
      else "Category: " + task.category
      
    soapClient.createSubissue(projectId, parentId,
      summary = task.description,
      description = task.description + " (" + category + ")\n" +
        task.subtasks.map("- " + _.description).mkString("\n"),
        config.subissuetypename)
  }
}
