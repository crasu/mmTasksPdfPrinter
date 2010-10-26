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
    val actForValid = MmParser invokePrivate sanityCheck(validData)
    it("must be able to detect valid data") {
      actForValid must be (true)
    }
    it("and invalid data") {
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
    val extractScrumPoints = PrivateMethod[Int]('extractScrumPoints)

    val descBrackets = <node TEXT="   Sprint 2010-20 (123 pts) SomeMoreText   " />
    val expBrackets = 123
    it("must be able to parse points in brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descBrackets)
      act must be (expBrackets)
    }

    val descCurely = <node TEXT="   Sprint 2010-20 { 7 pts} SomeMoreText   " />
    val expCurely = 7
    it("must be able to parse points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely)
      act must be (expCurely)
    }

    val descCombined = <node TEXT="   Sprint 2010-20 (123 pts) (5 beers) SomeMoreText   " />
    val expCombined = 5
    it("must be able to parse points even if it is ambiguous") {
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
    it("must be able to parse all subtasks of a task") {
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
    val exp = List(Story("foo", Story.NO_ESTIMATION, 1),
                   Story("bar \"foobar\"", Story.NO_ESTIMATION, 2))
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
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="NoSprint"/>
      </map>""")

    val traverseBacklogs = PrivateMethod[Seq[SprintBacklog]]('traverseBacklogs)
    val exp = List(SprintBacklog("2010-20"),
                   SprintBacklog("2010-21"))
    it("must be able to detect every sprint") {
      val act = MmParser invokePrivate traverseBacklogs(root)
      act.toList must be (exp)
    }
  }

  describe("MmParser") {
    val exp = List(
      {
        var sb = SprintBacklog("2010-20")
        sb.stories += {
          var s = Story("asdf", Story.NO_ESTIMATION, 1)
          s.tasks += Task("foo", "")
          s.tasks += Task("bar \"foobar\"", "")
          s
        }
        sb
      },
      {
        var sb = SprintBacklog("2010-21")
        sb.stories += {
          var s = Story("Some Story: A tale about...", 29, 1)
          s.tasks += Task("buy Mindstorms set", "Dev")
          s.tasks += {
            var t = Task("write remote control perl script", "Dev")
            t.subtasks += Subtask("write unit tests")
            t.subtasks += Subtask("write module mod1")
            t.subtasks += Subtask("write module mod2 part a")
            t.subtasks += Subtask("write module mod2 part b")
            t
          }
          s.tasks += Task("install replacement firmware", "Dev")
          s.tasks += Task("regression", "CT")
          s.tasks += Task("deploy to production", "Deployment")
          s
        }
        sb.stories += {
         var s = Story("Another Story", 30, 2)
         s.tasks += Task("Do one thing", "")
         s.tasks += Task("do another thing", "")
         s.tasks += {
           var t = Task("task1", "cat subcat1")
           t.subtasks += Subtask("subtask1")
           t.subtasks += Subtask("subtask2")
           t.subtasks += Subtask("subtask3")
           t.subtasks += Subtask("subtask4")
           t.subtasks += Subtask("subtask5")
           t.subtasks += Subtask("subtask6")
           t.subtasks += Subtask("subtask7")
           t
         }
         s.tasks += Task("task2", "cat subcat1")
         s.tasks += Task("taskX", "cat subcat2")
         s.tasks += Task("\"taskX\"Hallo&<NANA>", "cat subcat2")
         s
        }
        sb
      },
      {
        var sb = SprintBacklog("2010-22")
        sb.stories += Story("Some huge story With some big story Some medium sized story1 And finally: A story1", Story.NO_ESTIMATION, 1)
        sb.stories += Story("Some huge story With some big story Some medium sized story1 And finally: A story2", Story.NO_ESTIMATION, 2)
        sb.stories += Story("Some huge story With some big story Some medium sized story2 And finally: A story1", Story.NO_ESTIMATION, 3)
        sb.stories += Story("Some huge story With some big story Some medium sized story2 And finally: A story2", Story.NO_ESTIMATION, 4)
        sb.stories += Story("Some huge story And another big story Another story1", Story.NO_ESTIMATION, 5)
        sb.stories += Story("Some huge story And another big story Another story2", Story.NO_ESTIMATION, 6)
        sb.stories += Story("Some huge story And yet another big story Yet another story1", Story.NO_ESTIMATION, 7)
        sb.stories += Story("Some huge story And yet another big story Yet another story2", Story.NO_ESTIMATION, 8)
        sb
      }
    )
    it("must parse a xml file to an internal data structure") {
      val act = MmParser.parse(validData)
      act.toList must be (exp)
    }
  }
}
