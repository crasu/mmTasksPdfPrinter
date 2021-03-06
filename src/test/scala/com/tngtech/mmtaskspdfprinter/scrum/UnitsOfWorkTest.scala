package com.tngtech.mmtaskspdfprinter.scrum

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester
import Dsl._

@RunWith(classOf[JUnitRunner])
class UnitsOfWorkTest extends Spec with MustMatchers {
  describe("A task") {
    val t = 
      Task("A task", "Dev",  List(
        Subtask("First subtask"),
        Subtask("Second subtask"))
      )

    val exp = List(Subtask("First subtask"),
                   Subtask("Second subtask"))

    it("must keep the order of its subtasks") {
      t.subtasks must be (exp)
    }
  }
  
  describe("The TaskIdGenerator") {
    it("must generate the hashcode") {
      TaskIdGenerator.generate("plaintext") must be ("GNDEW")
    }
  }

  describe("A story") {
     val s = 
       Story("A story", UndefScrumPoints, None,
         List(Task("First task", "Dev"),
         Task("Second task", "CT"))
       )

     val exp = List(Task("First task", "Dev"),
                    Task("Second task", "CT"))

    it("must keep the order of its tasks") {
      s.tasks must be (exp)
    }
  }

  describe("A story") {
    it("must recognize equal stories") {
      Story("One", UndefScrumPoints, None) must be (Story("One", UndefScrumPoints, None))
      Story("One", 16 pts, NoPrio) must be (Story("One", 16 pts, NoPrio))
      Story("One", 16 pts, 1 prio) must be (Story("One", 16 pts, 1 prio))
    }
    it("must recognize different stories") {
      Story("One", UndefScrumPoints, NoPrio) must not be (Story("One", 1.point, NoPrio))
      Story("One", 2 pts, NoPrio) must not be (Story("One", 1 pt, NoPrio))
      Story("One", 16 pts, NoPrio) must not be (Story("Two", 16 pts, NoPrio))
      Story("One", 16 pts, 2 prio) must not be (Story("Two", 16 pts, 1 prio))
    }
  }

  describe("A sprint backlog") {
     val s = 
       Sprint("A sprint backlog", 
         Story("First story"),
         Story("Second story")
       )

     val exp = List(Story("First story"),
                    Story("Second story"))

    it("must keep the order of its stories") {
      s.stories must be (exp)
    }
  }
  
  describe("ScrumPoints") {
     
    it("must print to String correctly") {
      IntScrumPoints(10).toString("def") must be("10")
    }
  }
}