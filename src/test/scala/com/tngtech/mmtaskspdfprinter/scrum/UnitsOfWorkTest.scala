package com.tngtech.mmtaskspdfprinter.scrum

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester

@RunWith(classOf[JUnitRunner])
class UnitsOfWorkTest extends Spec with MustMatchers {
  describe("A task") {
    val t = 
      Task("A task", "Dev", 
        Subtask("First subtask"),
        Subtask("Second subtask")
      )

    val exp = List(Subtask("First subtask"),
                   Subtask("Second subtask"))

    it("must keep the order of the subtasks") {
      t.subtasks must be (exp)
    }
  }

  describe("A story") {
     val s = 
       Story("A story", None, None,
         Task("First task", "Dev"),
         Task("Second task", "CT")
       )

     val exp = List(Task("First task", "Dev"),
                    Task("Second task", "CT"))

    it("must keep the order of the tasks") {
      s.tasks must be (exp)
    }
  }

  describe("A story") {
    it("must be equal") {
      Story("One", None, None) must be (Story("One", None, None))
      Story("One", 16, None) must be (Story("One", 16, None))
      Story("One", 16, 1) must be (Story("One", 16, 1))
    }
    it("must not be equal") {
      Story("One", None, None) must not be (Story("One", 1, None))
      Story("One", 2, None) must not be (Story("One", 1, None))
      Story("One", 16, None) must not be (Story("Two", 16, None))
      Story("One", 16, 2) must not be (Story("Two", 16, 1))
    }
  }

  describe("A sprint backlog") {
     val s = 
       SprintBacklog("A sprint backlog", 
         Story("First story", None, None),
         Story("Second story", None, None)
       )

     val exp = List(Story("First story", None, None),
                    Story("Second story", None, None))

    it("must keep the order of the stories") {
      s.stories must be (exp)
    }
  }
}