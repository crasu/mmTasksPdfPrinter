package com.tngtech.mmtaskspdfprinter.parsing

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.PrivateMethodTester
import scala.xml._

import com.tngtech.mmtaskspdfprinter.scrum._

@RunWith(classOf[JUnitRunner])
class MmParserTest extends Spec with MustMatchers with PrivateMethodTester {

  val validData = XML.loadString(MmParserTestData.validData)
  val invalidData = XML.loadString(MmParserTestData.invalidData)

  describe("MmParser sanity check") {
    val sanityCheck = PrivateMethod[Boolean]('sanityCheck)
    it("must detect valid data") {
      val actForValid = MmParser invokePrivate sanityCheck(validData)
      actForValid must be (true)
    }
    it("must detect invalid data") {
      val actForInvalid = MmParser invokePrivate sanityCheck(invalidData)
      actForInvalid must be (false)
    }
  }

  describe("MmParser description extractor") {
    val desc = <node TEXT="    Sprint 2010-20 (123 pts) {5 beers} SomeMoreText    " />
    val extractDescription = PrivateMethod[String]('extractDescription)
    val exp = "Sprint 2010-20 SomeMoreText"
    it("must remove things in brackets and whitespaces") {
      val act = MmParser invokePrivate extractDescription(desc)
      act must be (exp)
    }
  }

  describe("MmParser scrum points extractor") {
    val extractScrumPoints = PrivateMethod[ScrumPoints]('extractScrumPoints)

    val descBrackets = <node TEXT="   Sprint 2010-20 (123 pts) SomeMoreText   " />
    val expBrackets = IntScrumPoints(123)
    it("must parse points in brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descBrackets)
      act must be (expBrackets)
    }

    val descCurely = <node TEXT="   Sprint 2010-20 { 7 pts} SomeMoreText   " />
    val expCurely = IntScrumPoints(7)
    it("must parse points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely)
      act must be (expCurely)
    }
    
    val descCurely05 = <node TEXT="   Sprint 2010-20 { 0.5 pts} SomeMoreText   " />
    it("must parse 0.5 points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely05)
      act must be (HalfScrumPoint)
    }
    
    val descCurely05Comma = <node TEXT="   Sprint 2010-20 { 0,5 pts} SomeMoreText   " />
    it("must parse 0,5 points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely05Comma)
      act must be (HalfScrumPoint)
    }

    val descCombined = <node TEXT="   Sprint 2010-20 (123 pts) (5 beers) SomeMoreText   " />
    val expCombined = IntScrumPoints(5)
    it("must parse points even if it is ambiguous") {
      val act = MmParser invokePrivate extractScrumPoints(descCombined)
      act must be (expCombined)
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
      val traverseSubtasks = PrivateMethod[Seq[Subtask]]('traverseSubtasks)
      val subtasks = MmParser invokePrivate traverseSubtasks(subtaskTree)
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
      val subtasks = MmParser.traverseSubtasks(subtaskTree)
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
      val act = MmParser.traverseTasks(story)
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

      val exp = List(SprintBacklog("Sprint 2010-20"),
        SprintBacklog("Sprint 2010-21"),
        SprintBacklog("Sprint 43"),
        SprintBacklog("Product Backlog"),
        SprintBacklog("Backlog"),
        SprintBacklog("backlog"))
      val act = MmParser.traverseBacklogs(root)
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

      val exp = List(SprintBacklog("Sprint 2010-20",
        List(Story("csasd 2412432", UndefScrumPoints, Some(1))): _*))
      val act = MmParser.parse(root)
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
          </node>
        </node>
        val exp = List(
            Story("a",IntScrumPoints(5),Some(1)),
            Story("b",IntScrumPoints(3),Some(2)),
            Story("c",UndefScrumPoints,Some(3)))
        MmParser.traverseStories(xml) must be (exp)
    }
  }

  describe("MmParser") {
    val exp = List(
      SprintBacklog("Sprint 2010-20", List( 
        Story("asdf", UndefScrumPoints, 1, 
          List(Task("foo", ""), Task("bar \"foobar\"", ""))
        )):_*
      ),
      SprintBacklog("Sprint 2010-21", List(
        Story("Some Story: A tale about...", 29, 1, 
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
        Story("Another Story", 30, 2, List(
          Task("Do one thing", ""),
          Task("do another thing", ""),
          Task("task1", "cat subcat1", List(
            Subtask("subtask1"), Subtask("subtask2"), Subtask("subtask3"),
            Subtask("subtask4"), Subtask("subtask5"), Subtask("subtask6"),
            Subtask("subtask7"))
          ),
          Task("task2", "cat subcat1"),
          Task("taskX", "cat subcat2"),
          Task("\"taskX\"Hallo&<NANA>", "cat subcat2"))
        )):_*
      ),
      SprintBacklog("Sprint 2010-22", List(
        Story("Story leaf 1-1", UndefScrumPoints, 1), 
        Story("Story leaf 1-2", UndefScrumPoints, 2),
        Story("Story leaf 2-1", UndefScrumPoints, 3), 
        Story("Story leaf 2-2", UndefScrumPoints, 4),
        Story("Story leaf 3-1", UndefScrumPoints, 5), 
        Story("Story leaf 3-2", UndefScrumPoints, 6),
        Story("Yet another leaf 4-1", UndefScrumPoints, 7), 
        Story("Yet another leaf 4-2", UndefScrumPoints, 8)
      ):_*)
    )
    it("must parse a xml file to an internal data structure") {
      val act = MmParser.parse(validData)
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
