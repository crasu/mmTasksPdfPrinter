package com.tngtech.mmtaskspdfprinter.creation.jira

import org.specs.Specification
import org.junit.runner.RunWith
import org.specs.runner.{ JUnitSuiteRunner, JUnit }
import org.specs.SpecificationWithJUnit
import org.specs.mock.Mockito
import config.JiraConfiguration
import com.tngtech.mmtaskspdfprinter.scrum.Story
import com.tngtech.mmtaskspdfprinter.scrum.Task
import com.tngtech.mmtaskspdfprinter.scrum.Sprint
import com.tngtech.mmtaskspdfprinter.scrum.Subtask
import org.mockito.Matchers._
import com.tngtech.mmtaskspdfprinter.scrum.Dsl._
import com.sun.xml.internal.ws.fault.SOAP11Fault

@RunWith(classOf[JUnitSuiteRunner])
class JiraTaskCreatorTest extends Specification with Mockito {
  "Creation of issues and subissues in JIRA" should {
    "call the right functions from the JIRA API and retrieve the JIRA keys" in {
      val soap = mock[SoapClient]
      val conf = mock[JiraConfiguration]
      soap.createIssue(anyString(), ==("Story1"), anyString()) returns "jiraStory1"
      soap.createIssue(anyString(), ==("Story2"), anyString()) returns "jiraStory2"
      soap.createSubissue(anyString(), anyString(), ==("Task1-1"), anyString(), anyString()) returns "jiraTask1-1"
      soap.createSubissue(anyString(), anyString(), ==("Task2-1"), anyString(), anyString()) returns "jiraTask2-1"
      soap.createSubissue(anyString(), anyString(), ==("Task2-2"), anyString(), anyString()) returns "jiraTask2-2"

      val jc = new JiraTaskCreator(conf, soap, "pid")

      val sprint = 
        Sprint("TheBacklog", 
	        Story("Story1", 1 point, 2 prio, List(
	          Task("Task1-1", "cat"))),
	        Story("Story2", 1 point, 2 prio, List(
	          Task("Task2-1", "dog"),
	          Task("Task2-2", "camel", List(
	            Subtask("Subtask2-2-1"),
	            Subtask("Subtask2-2-2"),
	            Subtask("Subtask2-2-3"),
	            Subtask("Subtask2-2-4"))))))
      val updatedSprint = jc.create(List(sprint))

      there were two(soap).createIssue(anyString(), anyString(), anyString())
      there were three(soap).createSubissue(anyString(), anyString(), anyString(), anyString(), anyString())
      
      updatedSprint.head.stories.map(_.jiraKey) must_== List("jiraStory1", "jiraStory2")
      val updatedTasks = for(sprint <- updatedSprint; story <- sprint.stories; task <- story.tasks) yield task
      updatedTasks.map(_.jiraKey) must_== List("jiraTask1-1", "jiraTask2-1", "jiraTask2-2")
    }
  }
}