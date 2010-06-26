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
    val desc = "   Sprint 2010-20 (123 pts) {5 beers} SomeMoreText   "
    val extractDescription = PrivateMethod[String]('extractDescription)
    val exp = "Sprint 2010-20 SomeMoreText"
    it("must remove things in brackets and whitespaces") {
      val act = MmParser invokePrivate extractDescription(desc)
      act must be (exp)
    }
  }

  describe("MmParser scrum points extractor") {
    val extractScrumPoints = PrivateMethod[Int]('extractScrumPoints)

    val descBrackets = "   Sprint 2010-20 (123 pts) SomeMoreText   "
    val expBrackets = 123
    it("must be able to parse points in brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descBrackets)
      act must be (expBrackets)
    }

    val descCurely = "   Sprint 2010-20 { 7 pts} SomeMoreText   "
    val expCurely = 7
    it("must be able to parse points in curely brackets") {
      val act = MmParser invokePrivate extractScrumPoints(descCurely)
      act must be (expCurely)
    }

    val descCombined = "   Sprint 2010-20 (123 pts) (5 beers) SomeMoreText   "
    val expCombined = 5
    it("must be able to parse points even if it is ambiguous") {
      val act = MmParser invokePrivate extractScrumPoints(descCombined)
      act must be (expCombined)
    }
  }

  describe("MmParser") {
    val aTaskNode = XML.loadString("""
      <node CREATED="1269526450124" ID="Freemind_Link_1309764532" MODIFIED="1269529996041" TEXT="bar &quot;foobar&quot;">
        <icon BUILTIN="attach"/>
      </node>""")
    val isTask = PrivateMethod[Boolean]('isTask)
    it("must be able to tell if a node represents a task") {
      val aTaskNodeIsATask = MmParser invokePrivate isTask(aTaskNode)
      aTaskNodeIsATask must be (true)
    }
    val notATaskNode = XML.loadString(""" 
      <node CREATED="1269526450124" ID="Freemind_Link_1309764532" MODIFIED="1269529996041" TEXT="bar &quot;foobar&quot;">
        <icon BUILTIN="somethingDifferent"/>
      </node>""")
    it("or if it is not a task") {
      val notATaskNodeIsATask = MmParser invokePrivate isTask(notATaskNode)
      notATaskNodeIsATask must be (false)
    }
  }

  describe("MmParser") {
    val subtaskTree = XML.loadString("""
      <node CREATED="1265988639762" ID="ID_713677349" MODIFIED="1265988645059" TEXT="write module">
        <node CREATED="1269529671283" ID="Freemind_Link_662706228" MODIFIED="1269529673077" TEXT="mod1"/>
        <node CREATED="1269529673537" ID="Freemind_Link_775829959" MODIFIED="1269529675471" TEXT="mod2">
          <node CREATED="1269529676056" ID="Freemind_Link_841377711" MODIFIED="1269529680690" TEXT="part a"/>
          <node CREATED="1269529680994" ID="Freemind_Link_437477332" MODIFIED="1269529682616" TEXT="part b"/>\n\
        </node>
      </node>""")
    val exp = List(Subtask("write module mod1"),
                  Subtask("write module mod2 part a"),
                  Subtask("write module mod2 part b"))
    val traverseSubtasks = PrivateMethod[Seq[Subtask]]('traverseSubtasks)
    it("must be able to parse all subtasks of a task") {
      val subtasks = MmParser invokePrivate traverseSubtasks("", subtaskTree)
      subtasks.toList must be (exp)
    }
  }

  describe("MmParser") {
    val taskTree = XML.loadString("""
      <node CREATED="1265988596571" ID="ID_389219738" MODIFIED="1269597472370" TEXT="write remote control perl script (8)">
        <icon BUILTIN="attach"/>
        <node CREATED="1265988639762" ID="ID_713677349" MODIFIED="1265988645059" TEXT="write module"/>
      </node>""")
    val extractTask = PrivateMethod[Task]('extractTask)
    val exp = Task("write remote control perl script", "Cat")
    exp.subtasks += Subtask("write module")
    it("must be able to parse a task") {
      val act = MmParser invokePrivate extractTask("Cat", taskTree)
      act must be (exp)
    }
  }

  describe("MmParser") {
    val catTree = XML.loadString("""
      <node CREATED="1265988501967" ID="ID_1335473995" MODIFIED="1272014962570" TEXT="cat {29}">
        <node CREATED="1265988549931" ID="ID_316971051" MODIFIED="1272014950967" TEXT="subcat{16}">
          <node CREATED="1265988596571" ID="ID_389219738" MODIFIED="1269597472370" TEXT="write remote control perl script (8)">
            <icon BUILTIN="attach"/>
          </node>
        </node>
      </node>""")
    val traverseCategories = PrivateMethod[Seq[Task]]('traverseCategories)
    val exp = List(Task("write remote control perl script", 
                            "cat subcat"))
    it("must be able to parse a category") {
      val act = MmParser invokePrivate traverseCategories("", catTree)
      act.toList must be (exp)
    }
  }

  describe("MmParser") {
    val storyTree = XML.loadString("""
      <node CREATED="1269526441170" ID="Freemind_Link_96745043" MODIFIED="1269529884698" TEXT="asdf (15)">
        <icon BUILTIN="full-1"/>
        <node CREATED="1269526448681" ID="Freemind_Link_1892252504" MODIFIED="1269526465865" TEXT="foo">
          <icon BUILTIN="attach"/>
        </node>
        <node CREATED="1269526450124" ID="Freemind_Link_1309764532" MODIFIED="1269529996041" TEXT="bar &quot;foobar&quot;">
          <icon BUILTIN="attach"/>
        </node>
      </node>""")
    val traverseStory = PrivateMethod[Story]('traverseStory)
    val exp = Story("asdf", 15, 1)
    exp.tasks += Task("foo", "")
    exp.tasks += Task("bar \"foobar\"", "")
    it("must be able to parse stories") {
      val act = MmParser invokePrivate traverseStory(storyTree, 1)
      act must be (exp)
    }
  }

  describe("MmParser") {
    val sprintTree = XML.loadString("""
      <node CREATED="1265988535423" ID="ID_692299973" MODIFIED="1272014974692" POSITION="right" TEXT="Sprint 2010-21 (59)">
        <node CREATED="1265988501967" ID="ID_1335473995" MODIFIED="1272014962570" TEXT="Some Story: A tale about... {29}"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="Another Story (30)"/>\n\
        <node CREATED="12695050169765" ID="ID_474350440" MODIFIED="1269597503934" TEXT="Equals Story (5/10/23/11 = 49pts)"/>
      </node>""")

    val traverseSprint = PrivateMethod[SprintBacklog]('traverseSprint)
    val exp = SprintBacklog("2010-21")
    exp.stories += Story("Some Story: A tale about...", 29, 1)
    exp.stories += Story("Another Story", 30, 2)
    exp.stories += Story("Equals Story", 49, 3)
    it("must be able to parse a sprint") {
      val act = MmParser invokePrivate traverseSprint(sprintTree, "2010-21")
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

    val traverseBacklog = PrivateMethod[Seq[SprintBacklog]]('traverseBacklog)
    val exp = List(SprintBacklog("2010-20"),
                   SprintBacklog("2010-21"))
    it("must be able to detect every sprint") {
      val act = MmParser invokePrivate traverseBacklog(root)
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
        sb.stories += Story("csasd", Story.NO_ESTIMATION, 2)
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
         s
        }
        sb
      }
    )
    it("must parse a xml file to an internal data structure") {
      val act = MmParser.parse(validData)
      act.toList must be (exp)
    }
  }
}
