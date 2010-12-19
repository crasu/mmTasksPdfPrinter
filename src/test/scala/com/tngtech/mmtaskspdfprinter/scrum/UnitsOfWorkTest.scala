package com.tngtech.mmtaskspdfprinter.scrum

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester

@RunWith(classOf[JUnitRunner])
class UnitsOfWorkTest extends Spec with MustMatchers {
  describe("A task") {
    val t = Task("A task", "Dev")
    t.subtasks :+= Subtask("First subtask")
    t.subtasks :+= Subtask("Second subtask")

    val exp = List(Subtask("First subtask"),
                   Subtask("Second subtask"))

    it("must maintain the order of the subtasks") {
      t.subtasks must be (exp)
    }
  }

  describe("A story") {
     val s = Story("A story")
     s.tasks :+= Task("First task", "Dev")
     s.tasks :+= Task("Second task", "CT")

     val exp = List(Task("First task", "Dev"),
                    Task("Second task", "CT"))

    it("must maintain the order of the tasks") {
      s.tasks must be (exp)
    }
  }

  describe("A story") {
    it("must be equal") {
      Story("One") must be (Story("One"))
      Story("One", 16) must be (Story("One", 16))
      Story("One", 16, 1) must be (Story("One", 16, 1))
    }
    it("must not be equal") {
      Story("One") must not be (Story("One"), 1)
      Story("One", 2) must not be (Story("One"), 1)
      Story("One", 16) must not be (Story("Two", 16))
      Story("One", 16, 2) must not be (Story("Two", 16, 1))
    }
  }

  describe("A sprint backlog") {
     val s = SprintBacklog("A sprint backlog")
     s.stories :+= Story("First story")
     s.stories :+= Story("Second story")

     val exp = List(Story("First story"),
                    Story("Second story"))

    it("must maintain the order of the stories") {
      s.stories must be (exp)
    }
  }
}