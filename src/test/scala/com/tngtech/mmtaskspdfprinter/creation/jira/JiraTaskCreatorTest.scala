package com.tngtech.mmtaskspdfprinter.creation.jira

import org.specs.Specification
import org.junit.runner.RunWith
import org.specs.runner.{ JUnitSuiteRunner, JUnit }
import org.specs.SpecificationWithJUnit
import org.specs.mock.Mockito
import config.JiraConfiguration
import com.tngtech.mmtaskspdfprinter.scrum.Story
import com.tngtech.mmtaskspdfprinter.scrum.Task
import com.tngtech.mmtaskspdfprinter.scrum.SprintBacklog
import com.tngtech.mmtaskspdfprinter.scrum.Subtask
import org.mockito.Matchers._

@RunWith(classOf[JUnitSuiteRunner])
class JiraTaskCreatorTest extends Specification with Mockito {
	"Creation" should {
	  "work" in {
	    val rpc = mock[RpcClient]
	    val rest = mock[RestClient]
	    val conf = mock[JiraConfiguration]
	    rpc.findProjectId("pid") returns "pid123"
	    rpc.createIssue(anyString(), anyString()) returns RpcResponse("", "") 
	    
	    val jc = new JiraTaskCreator(conf, rpc, rest, "pid")
	    
	    val t2 = Task("t1", "cat", Subtask("123"), Subtask("124"), Subtask("125"), Subtask("126"))
	    val s1 = Story("1", Some(1), Some(2), Task("t1", "cat"))
	    val s2 = Story("1", Some(1), Some(2), Task("t1", "cat"), t2)
	    jc.create(List(SprintBacklog("backlog", s1, s2)))
	    
	    there were two(rpc).createIssue(anyString(), anyString()) 
	    there were three(rest).createSubissue(anyString(), anyString(), anyString(), anyString(), anyString())
	  }
	}
}