package com.tngtech.mmtaskspdfprinter

import com.tngtech.mmtaskspdfprinter.parsing._
import scala.xml._
import com.tngtech.mmtaskspdfprinter.scrum._

object Main {
	def main(args: Array[String]): Unit = {
	  List(
      SprintBacklog("Sprint 2010-20", 
        Story("asdf", UndefScrumPoints, 1, 
          List(Task("foo", ""), Task("bar \"foobar\"", ""))
        )
      ),
      SprintBacklog("Sprint 2010-21", 
        Story("Some Story: A tale about...", 29, 1, 
          List(Task("buy Mindstorms set", "Dev"),
	          Task("write remote control perl script", "Dev", List( 
	            Subtask("write unit tests"),
	            Subtask("write module mod1"),
	            Subtask("write module mod2 part a"),
	            Subtask("write module mod2 part b")
	          )),
	          Task("install replacement firmware", "Dev"),
	          Task("regression", "CT"),
	          Task("deploy to production", "Deployment"))
        ),
        Story("Another Story", 30, 2, List(
          Task("Do one thing", ""),
          Task("do another thing", ""),
          Task("task1", "cat subcat1",  List(
            Subtask("subtask1"), Subtask("subtask2"), Subtask("subtask3"),
            Subtask("subtask4"), Subtask("subtask5"), Subtask("subtask6"),
            Subtask("subtask7")
          )),
          Task("task2", "cat subcat1"),
          Task("taskX", "cat subcat2"),
          Task("\"taskX\"Hallo&<NANA>", "cat subcat2"))
        )
      ),
      SprintBacklog("Sprint 2010-22", List(
        Story("Story leaf 1-1", UndefScrumPoints, 1), 
        Story("Story leaf 1-2", UndefScrumPoints, 2),
        Story("Story leaf 2-1", UndefScrumPoints, 3), 
        Story("Story leaf 2-2", UndefScrumPoints, 4),
        Story("Story leaf 3-1", UndefScrumPoints, 5), 
        Story("Story leaf 3-2", UndefScrumPoints, 6),
        Story("Yet another leaf 4-1", UndefScrumPoints, 7), 
        Story("Yet another leaf 4-2", UndefScrumPoints, 8)):_*)
      )
	}
}