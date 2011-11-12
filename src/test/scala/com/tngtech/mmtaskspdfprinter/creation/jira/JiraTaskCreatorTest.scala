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

@RunWith(classOf[JUnitSuiteRunner])
class JiraTaskCreatorTest extends Specification with Mockito {
  "Creation of issues and subissues in JIRA" should {
    "call the right functions from the JIRA API and retrieve the JIRA keys" in {
      val rpc = mock[RpcClient]
      val rest = mock[RestClient]
      val conf = mock[JiraConfiguration]
      rpc.findProjectId("pid") returns "pid123"
      rpc.createIssue(anyString(), ==("Story1"), anyString()) returns RpcResponse("", "jiraStory1")
      rpc.createIssue(anyString(), ==("Story2"), anyString()) returns RpcResponse("", "jiraStory2")
      rest.createSubissue(anyString(), anyString(), ==("Task1-1"), anyString(), anyString()) returns "jiraTask1-1"
      rest.createSubissue(anyString(), anyString(), ==("Task2-1"), anyString(), anyString()) returns "jiraTask2-1"
      rest.createSubissue(anyString(), anyString(), ==("Task2-2"), anyString(), anyString()) returns "jiraTask2-2"

      val jc = new JiraTaskCreator(conf, rpc, rest, "pid")

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

      there were two(rpc).createIssue(anyString(), anyString(), anyString())
      there were three(rest).createSubissue(anyString(), anyString(), anyString(), anyString(), anyString())
      
      updatedSprint.head.stories.map(_.jiraKey) must_== List("jiraStory1", "jiraStory2")
      val updatedTasks = for(sprint <- updatedSprint; story <- sprint.stories; task <- story.tasks) yield task
      updatedTasks.map(_.jiraKey) must_== List("jiraTask1-1", "jiraTask2-1", "jiraTask2-2")
    }
  }
}