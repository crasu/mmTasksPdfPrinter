package com.tngtech.mmtaskspdfprinter.creation.jira

import scala.collection.JavaConversions._
import config._

import com.tngtech.mmtaskspdfprinter.scrum._

class JiraTaskCreator(val config: JiraConfiguration, val projectName: String,
    val soapClient: JiraSoapMessages) {
  
  def this (config: JiraConfiguration, projectName: String,
        url: String, user: String, password: String) = 
        	this(config, projectName,
        	    new JiraSoapMessages(url, user, password))

  def create(backlogs: List[Sprint]): List[Sprint] = 
  try {
    soapClient.loginToProject(projectName, config.issuetypename, config.subissuetypename)
    createBacklog(backlogs)
  }
  finally {
    soapClient.logout
  }
  
  private def createBacklog(backlogs: List[Sprint]) = {
    val backlogsWithKeys = for (backlog <- backlogs) yield {
      val storiesWithKeys = for (story <- backlog.stories) yield {
      	createStory(story)
      }
      backlog.copy(stories = storiesWithKeys)
    }
    backlogsWithKeys          
  }
  
  private def createStory(story: Story): Story = {
    val issueKey = createIssue(story)
    val tasksWithKeys = for (task <- story.tasks) yield {
      val subissueKey = createSubissue(issueKey, story, task)
      task.copy(jiraKey = subissueKey) 
    }
    story.copy(jiraKey = issueKey, tasks = tasksWithKeys)
  }
  
  private def createIssue(story: Story): String = {
	if (!story.acceptanceCriteria.isEmpty){
    		soapClient.createIssue(story.name, description = "acceptance criteria for " + story.name + ":\n"
		  + story.acceptanceCriteria.map("- " + _).mkString("\n"))
  	} else {
    		soapClient.createIssue(story.name, description = "acceptance criteria for " + story.name + ":\n"
		   + "None")
  	}
  }
	
  private def createSubissue(parentId: String, story: Story, task: Task): String = {
    val category =
      if (task.category.isEmpty) ""
      else "Category: " + task.category
      
    soapClient.createSubissue(parentId,
      summary = task.description,
      description = task.description + " (" + category + ")\n" +
        task.subtasks.map("- " + _.description).mkString("\n"))
  }
}

