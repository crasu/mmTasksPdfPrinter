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
    val extractScrumPoints = PrivateMethod[Option[Int]]('extractScrumPoints)

    val descBrackets = <node TEXT="   Sprint 2010-20 (123 pts) SomeMoreText   " />
    val expBrackets = Some(123)
    it("must parse points in brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descBrackets)
      act must be (expBrackets)
    }

    val descCurely = <node TEXT="   Sprint 2010-20 { 7 pts} SomeMoreText   " />
    val expCurely = Some(7)
    it("must parse points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely)
      act must be (expCurely)
    }

    val descCombined = <node TEXT="   Sprint 2010-20 (123 pts) (5 beers) SomeMoreText   " />
    val expCombined = Some(5)
    it("must parse points even if it is ambiguous") {
      val act = MmParser invokePrivate extractScrumPoints(descCombined)
      act must be (expCombined)
    }
  }

  describe("MmParser") {
    val subtaskTree = XML.loadString("""
      <node CREATED="1265988639753" ID="ID_713677348" MODIFIED="1265988645059" TEXT="My Task">
      <icon BUILTIN="bookmark"/>
        <node CREATED="1265988639762" ID="ID_713677349" MODIFIED="1265988645059" TEXT="write module">
          <node CREATED="1269529671283" ID="Freemind_Link_662706228" MODIFIED="1269529673077" TEXT="mod1"/>
          <node CREATED="1269529673537" ID="Freemind_Link_775829959" MODIFIED="1269529675471" TEXT="mod2">
            <node CREATED="1269529676056" ID="Freemind_Link_841377711" MODIFIED="1269529680690" TEXT="part a"/>
            <node CREATED="1269529680994" ID="Freemind_Link_437477332" MODIFIED="1269529682616" TEXT="part b"/>\n\
          </node>
        </node>
      </node>""")
    val exp = List(Subtask("write module mod1"),
                   Subtask("write module mod2 part a"),
                   Subtask("write module mod2 part b"))
    val traverseSubtasks = PrivateMethod[Seq[Subtask]]('traverseSubtasks)
    it("must parse all subtasks of a task") {
      val subtasks = MmParser invokePrivate traverseSubtasks(subtaskTree)
      subtasks.toList must be (exp)
    }
  }

  describe("MmParser") {
    val storyTree = XML.loadString("""
      <node CREATED="1269526441170" ID="Freemind_Link_96745043" MODIFIED="1269529884698" TEXT="asdf (15)">
        <icon BUILTIN="full-1"/>
        <node CREATED="1269526448681" ID="Freemind_Link_1892252504" MODIFIED="1269526465865" TEXT="foo">
          <icon BUILTIN="bookmark"/>
        </node>
        <node CREATED="1269526450124" ID="Freemind_Link_1309764532" MODIFIED="1269529996041" TEXT="bar &quot;foobar&quot;">
          <icon BUILTIN="bookmark"/>
        </node>
      </node>""")
    val traverseStories = PrivateMethod[List[Story]]('traverseStories)
    val exp = List(Story("foo", None, 1),
                   Story("bar \"foobar\"", None, 2))
    it("must be able to parse stories") {
      val act = MmParser invokePrivate traverseStories(storyTree)
      act must be (exp)
    }
  }

  describe("MmParser") {
    val root = XML.loadString("""
      <map version="0.9.0">
        <node CREATED="1265988501967" ID="ID_1335473995" MODIFIED="1272014962570" TEXT="Sprint 2010-20"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="Sprint 2010-21"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="Sprint 43"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="something else"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" Product Backlog "/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" Backlog"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" backlog"/>
      </map>""")

    val traverseBacklogs = PrivateMethod[Seq[SprintBacklog]]('traverseBacklogs)
    val exp = List(SprintBacklog("Sprint 2010-20"),
                   SprintBacklog("Sprint 2010-21"),
                   SprintBacklog("Sprint 43"),
                   SprintBacklog("Product Backlog"),
                   SprintBacklog("Backlog"),
                   SprintBacklog("backlog"))
    it("must be able to detect every sprint") {
      val act = MmParser invokePrivate traverseBacklogs(root)
      act.toList must be (exp)
    }
  }

  describe("MmParser") {
    val exp = List(
      SprintBacklog("Sprint 2010-20", 
        Story("asdf", None, 1, 
          Task("foo", ""), Task("bar \"foobar\"", "")
        )
      ),
      SprintBacklog("Sprint 2010-21", 
        Story("Some Story: A tale about...", 29, 1, 
          Task("buy Mindstorms set", "Dev"),
          Task("write remote control perl script", "Dev", 
            Subtask("write unit tests"),
            Subtask("write module mod1"),
            Subtask("write module mod2 part a"),
            Subtask("write module mod2 part b")
          ),
          Task("install replacement firmware", "Dev"),
          Task("regression", "CT"),
          Task("deploy to production", "Deployment")
        ),
        Story("Another Story", 30, 2, 
          Task("Do one thing", ""),
          Task("do another thing", ""),
          Task("task1", "cat subcat1",
            Subtask("subtask1"), Subtask("subtask2"), Subtask("subtask3"),
            Subtask("subtask4"), Subtask("subtask5"), Subtask("subtask6"),
            Subtask("subtask7")
          ),
          Task("task2", "cat subcat1"),
          Task("taskX", "cat subcat2"),
          Task("\"taskX\"Hallo&<NANA>", "cat subcat2")
        )
      ),
      SprintBacklog("Sprint 2010-22",
        Story("Story leaf 1-1", None, 1), 
        Story("Story leaf 1-2", None, 2),
        Story("Story leaf 2-1", None, 3), 
        Story("Story leaf 2-2", None, 4),
        Story("Story leaf 3-1", None, 5), 
        Story("Story leaf 3-2", None, 6),
        Story("Yet another leaf 4-1", None, 7), 
        Story("Yet another leaf 4-2", None, 8)
      )
    )
    it("must parse a xml file to an internal data structure") {
      val act = MmParser.parse(validData)
      act.toList must be (exp)
    }
  }
}
