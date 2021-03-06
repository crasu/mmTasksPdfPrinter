package com.tngtech.mmtaskspdfprinter.parsing

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester
import scala.xml._

import com.tngtech.mmtaskspdfprinter.scrum._
import Dsl._

@RunWith(classOf[JUnitRunner])
class MmParserTest extends Spec with MustMatchers with PrivateMethodTester {

  val validData = XML.loadString(MmParserTestData.validData)
  val invalidData = XML.loadString(MmParserTestData.invalidData)
  
  val parser = new MmParser()

  describe("MmParser sanity check") {
    val sanityCheck = PrivateMethod[Boolean]('sanityCheck)
    it("must detect valid data") {
      val actForValid = parser invokePrivate sanityCheck(validData)
      actForValid must be (true)
    }
    it("must detect invalid data") {
      val actForInvalid = parser invokePrivate sanityCheck(invalidData)
      actForInvalid must be (false)
    }
  }

  describe("MmParser description extractor") {
    val desc = <node TEXT="    Sprint 2010-20 (123 pts) {5 beers} SomeMoreText    " />
    val extractDescription = PrivateMethod[String]('extractDescription)
    val exp = "Sprint 2010-20 SomeMoreText"
    it("must remove things in brackets and whitespaces") {
      val act = parser invokePrivate extractDescription(desc)
      act must be (exp)
    }
  }

  describe("MmParser scrum points extractor") {
    val extractScrumPoints = PrivateMethod[ScrumPoints]('extractScrumPoints)

    val descBrackets = <node TEXT="   Sprint 2010-20 (123 pts) SomeMoreText   " />
    it("must parse points in brackets") {
      val act = parser invokePrivate extractScrumPoints(descBrackets)
      act must be (123 pts)
    }

    val descCurely = <node TEXT="   Sprint 2010-20 { 7 pts} SomeMoreText   " />

    it("must parse points in curely brackets") {
      val act = parser invokePrivate extractScrumPoints(descCurely)
      act must be (7 pts)
    }
    
    val descCurely05 = <node TEXT="   Sprint 2010-20 { 0.5 pts} SomeMoreText   " />
    it("must parse 0.5 points in curely brackets") {
      val act = parser invokePrivate extractScrumPoints(descCurely05)
      act must be (HalfScrumPoint)
    }
    
    val descCurely05Comma = <node TEXT="   Sprint 2010-20 { 0,5 pts} SomeMoreText   " />
    it("must parse 0,5 points in curely brackets") {
      val act = parser invokePrivate extractScrumPoints(descCurely05Comma)
      act must be (HalfScrumPoint)
    }

    val descCombined = <node TEXT="   Sprint 2010-20 (123 pts) (5 beers) SomeMoreText   " />
    it("must parse points even if it is ambiguous") {
      val act = parser invokePrivate extractScrumPoints(descCombined)
      act must be (5 pts)
    }
  }

  describe("MmParser") {
    it("must parse all subtasks of a task") {
      val subtaskTree =
        <node TEXT="My Task">
          <icon BUILTIN="bookmark"/>
          <node TEXT="write module">
            <node TEXT="mod1"/>
            <node TEXT="mod2">
              <node TEXT="part a"/>
              <node TEXT="part b"/>
              \n\
            </node>
          </node>
        </node>
      val exp = List(Subtask("write module mod1"),
        Subtask("write module mod2 part a"),
        Subtask("write module mod2 part b"))
      val subtasks = parser.extractSubtasks(subtaskTree)
      subtasks.toList must be(exp)
    }
    it("must parse all tasks of a story") {

      val subtaskTree =
        <node TEXT="My Task">
          <icon BUILTIN="bookmark"/>
          <node TEXT="write module">
            <node TEXT="mod1"/>
            <node TEXT="mod2">
              <node TEXT="part a"/>
              <node TEXT="part b"/>
              \n\
            </node>
          </node>
        </node>
      val exp = List(Subtask("write module mod1"),
        Subtask("write module mod2 part a"),
        Subtask("write module mod2 part b"))
      val subtasks = parser.extractSubtasks(subtaskTree)
      subtasks.toList must be(exp)
    }
    
    it("must be able to parse stories") {
      val story =
        <node TEXT="asdf">
          <icon BUILTIN="full-1"/>
          <icon BUILTIN="bookmark"/>
          <node TEXT="foo">
            <icon BUILTIN="attach"/>
          </node>
          <node TEXT="bar &quot;foobar&quot;">
            <icon BUILTIN="attach"/>
          </node>
          <node TEXT="not in the output"/>
          <node TEXT="cat">
            <node TEXT="foo2">
              <icon BUILTIN="attach"/>
            </node>
          </node>
          <node TEXT="cat1">
            <node TEXT="cat2">
              <node TEXT="foo3">
                <icon BUILTIN="attach"/>
              </node>
            </node>
          </node>
        </node>
      val exp = List(
        Task("foo", ""),
        Task("bar \"foobar\"", ""),
        Task("foo2", "cat"),
        Task("foo3", "cat1 cat2"))
      val act = parser.extractTasksFromStory(story)
      act must be(exp)
    }

    it("must be able to detect every sprint") {
      val root =
        <map version="0.9.0">
          <node TEXT="Sprint 2010-20"/>
          <node TEXT="Sprint 2010-21"/>
          <node TEXT="Sprint 43"/>
          <node TEXT="something else"/>
          <node TEXT=" Product Backlog "/>
          <node TEXT=" Backlog"/>
          <node TEXT=" backlog"/>
        </map>

      val exp = List(Sprint("Sprint 2010-20"),
        Sprint("Sprint 2010-21"),
        Sprint("Sprint 43"),
        Sprint("Product Backlog"),
        Sprint("Backlog"),
        Sprint("backlog"))
      val act = parser.traverseBacklogs(root)
      act.toList must be(exp)
    }
    
    it("must be able to handle HTML nodes") {
      val root =
        <map version="0.9.0">
          <node TEXT="Product Backlog">
            <node POSITION="right" TEXT="Sprint 2010-20 (123 pts)">
              <node>
                <richcontent TYPE="NODE">
                  <html>
                    <head>
                    </head>
                    <body>
                      <p>
                        csasd <b>2412432</b>
                      </p>
                    </body>
                  </html>
                </richcontent>
                <icon BUILTIN="full-2"/>
                <icon BUILTIN="bookmark"/>
                <node TEXT="no task"/>
              </node>
            </node>
          </node>
        </map>

      val exp = List(Sprint("Sprint 2010-20",
        Story("csasd 2412432", priority = 1 st)))
      val act = parser.parse(root)
      act.toList must equal(exp)
    }
    
    it("must traverse stories") {
      val xml =
        <node TEXT="Sprint (123)">
          <node TEXT="Dev {16}">
            <node TEXT="a (5)">
              <icon BUILTIN="bookmark"/>
            </node>
            <node TEXT="b (3)">
              <icon BUILTIN="bookmark"/>
            </node>
          </node>
          <node TEXT="c">
            <icon BUILTIN="bookmark"/>
            <node TEXT="accept">
              <icon BUILTIN="list"/>
              <node TEXT="abc"/>
            </node>
          </node>
        </node>
        val exp = List(
            Story("a", 5 pts, priority = 1 st),
            Story("b", 3 pts, priority = 2 nd),
            Story("c", priority = 3 th, acceptanceCriteria = List("abc")))
        parser.extractStoriesFromSprint(xml) must be (exp)
    }
    
    it("must find acceptance criteria") {
      val xml =
        <node TEXT="story">
          <icon BUILTIN="bookmark"/>
          <node TEXT="foo">
            <icon BUILTIN="attach"/>
          </node>
          <node TEXT="accept1">
            <icon BUILTIN="list"/>
            <node TEXT="a1a"/>
            <node TEXT="a1b"/>
          </node>
          <node TEXT="accept2">
            <icon BUILTIN="list"/>
            <node TEXT="a2"/>
          </node>
          <node TEXT="accept3">
            <icon BUILTIN="list"/>
          </node>
        </node>
        val exp = List("a1a", "a1b", "a2")
        parser.extractAcceptanceCriteria(xml) must be (exp)
    }
  }

  describe("parser") {
    val exp = List(
      Sprint("Sprint 2010-20", List( 
        Story("asdf", priority = 1 st, 
          tasks = List(Task("foo", ""), Task("bar \"foobar\"", ""))
        ))
      ),
      Sprint("Sprint 2010-21", List(
        Story("Some Story: A tale about...", 29 points, 1 prio, 
          List(Task("buy Mindstorms set", "Dev"),
	          Task("write remote control perl script", "Dev",  List(
	            Subtask("write unit tests"),
	            Subtask("write module mod1"),
	            Subtask("write module mod2 part a"),
	            Subtask("write module mod2 part b"))
	          ),
	          Task("install replacement firmware", "Dev"),
	          Task("regression", "CT"),
	          Task("deploy to production", "Deployment"))
        ),
        Story("Another Story", 30 pts, 2 prio, List(
          Task("Do one thing", ""),
          Task("do another thing", ""),
          Task("task1", "cat subcat1", List(
            Subtask("subtask1"), Subtask("subtask2"), Subtask("subtask3"),
            Subtask("subtask4"), Subtask("subtask5"), Subtask("subtask6"),
            Subtask("subtask7"))
          ),
          Task("task2", "cat subcat1"),
          Task("taskX", "cat subcat2"),
          Task("\"taskX\"Hallo&<NANA>", "cat subcat2")),
          List("a", "b", "c", "d")
        ))
      ),
      Sprint("Sprint 2010-22", List(
        Story("Story leaf 1-1", priority = 1 st), 
        Story("Story leaf 1-2", priority = 2 nd),
        Story("Story leaf 2-1", priority = 3 rd), 
        Story("Story leaf 2-2", priority = 4 th),
        Story("Story leaf 3-1", priority = 5 th), 
        Story("Story leaf 3-2", priority = 6 th),
        Story("Yet another leaf 4-1", priority = 7 th), 
        Story("Yet another leaf 4-2", priority = 8 th)
      ))
    )
    it("must parse a xml file to an internal data structure") {
      val act = parser.parse(validData)
      for {(b1,b2) <- act zip exp} {
        b1.name must be (b2.name)
        b1.stories.size must be (b2.stories.size)
        for ((s1,s2) <- b1.stories zip b2.stories) {
          s1.name must be (s2.name)
        }
      }
      act.toList must be (exp)
    }
  }
}
