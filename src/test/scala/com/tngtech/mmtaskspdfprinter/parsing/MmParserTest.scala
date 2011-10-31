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
    val subtaskTree = 
      <node CREATED="1265988639753" ID="ID_713677348" MODIFIED="1265988645059" TEXT="My Task">
      <icon BUILTIN="bookmark"/>
        <node CREATED="1265988639762" ID="ID_713677349" MODIFIED="1265988645059" TEXT="write module">
          <node CREATED="1269529671283" ID="Freemind_Link_662706228" MODIFIED="1269529673077" TEXT="mod1"/>
          <node CREATED="1269529673537" ID="Freemind_Link_775829959" MODIFIED="1269529675471" TEXT="mod2">
            <node CREATED="1269529676056" ID="Freemind_Link_841377711" MODIFIED="1269529680690" TEXT="part a"/>
            <node CREATED="1269529680994" ID="Freemind_Link_437477332" MODIFIED="1269529682616" TEXT="part b"/>\n\
          </node>
        </node>
      </node>
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
    val subtaskTree = 
      <node CREATED="1265988639753" ID="ID_713677348" MODIFIED="1265988645059" TEXT="My Task">
      <icon BUILTIN="bookmark"/>
        <node CREATED="1265988639762" ID="ID_713677349" MODIFIED="1265988645059" TEXT="write module">
          <node CREATED="1269529671283" ID="Freemind_Link_662706228" MODIFIED="1269529673077" TEXT="mod1"/>
          <node CREATED="1269529673537" ID="Freemind_Link_775829959" MODIFIED="1269529675471" TEXT="mod2">
            <node CREATED="1269529676056" ID="Freemind_Link_841377711" MODIFIED="1269529680690" TEXT="part a"/>
            <node CREATED="1269529680994" ID="Freemind_Link_437477332" MODIFIED="1269529682616" TEXT="part b"/>\n\
          </node>
        </node>
      </node>
    val exp = List(Subtask("write module mod1"),
                   Subtask("write module mod2 part a"),
                   Subtask("write module mod2 part b"))
    val traverseSubtasks = PrivateMethod[Seq[Subtask]]('traverseSubtasks)
    it("must parse all tasks of a story") {
      val subtasks = MmParser invokePrivate traverseSubtasks(subtaskTree)
      subtasks.toList must be (exp)
    }
  }

  describe("MmParser") {
    val story = <node CREATED="1269526441170" ID="Freemind_Link_96745043" MODIFIED="1287734314282" TEXT="asdf">
                      <icon BUILTIN="full-1"/>
                      <icon BUILTIN="bookmark"/>
                      <node CREATED="1269526448681" ID="Freemind_Link_1892252504" MODIFIED="1269526465865" TEXT="foo">
                        <icon BUILTIN="attach"/>
                      </node>
                      <node CREATED="1269526450124" ID="Freemind_Link_1309764532" MODIFIED="1269529996041" TEXT="bar &quot;foobar&quot;">
                        <icon BUILTIN="attach"/>
                      </node>
                      <node CREATED="1269526453385" ID="Freemind_Link_866813631" MODIFIED="1269526457534" TEXT="not in the output"/>
                    </node>
    val exp = List(
        Task("foo", ""),
        Task("bar \"foobar\"", ""))
    it("must be able to parse stories") {
      val act = MmParser.traverseTasks(story)
      act must be (exp)
    }
  }

  describe("MmParser") {
    val root = 
      <map version="0.9.0">
        <node CREATED="1265988501967" ID="ID_1335473995" MODIFIED="1272014962570" TEXT="Sprint 2010-20"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="Sprint 2010-21"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="Sprint 43"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT="something else"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" Product Backlog "/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" Backlog"/>
        <node CREATED="1269505016971" ID="ID_474350437" MODIFIED="1269597503934" TEXT=" backlog"/>
      </map>

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
    val root = <map version="0.9.0">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1265988225850" ID="ID_204900544" MODIFIED="1269505504609" TEXT="Product Backlog">
<node CREATED="1269526293331" ID="Freemind_Link_879171622" MODIFIED="1272014836816" POSITION="right" TEXT="Sprint 2010-20 (123 pts)">
<node CREATED="1269526444157" ID="Freemind_Link_203734451" MODIFIED="1297930433713">
<richcontent TYPE="NODE"><html>
  <head>

  </head>
  <body>
    <p>
      csasd <b>2412432</b>
    </p>
  </body>
</html></richcontent>
<icon BUILTIN="full-2"/>
<icon BUILTIN="bookmark"/>
<node CREATED="1269526453385" ID="Freemind_Link_1753761911" MODIFIED="1297932020174" TEXT="no task"/>
</node>
</node>
</node>
</map>

    val traverseBacklogs = PrivateMethod[Seq[SprintBacklog]]('traverseBacklogs)
    val exp = List(SprintBacklog("Sprint 2010-20", Story("csasd 2412432", UndefScrumPoints, Some(1))))
    it("must be able to handle HTML nodes") {
      val act = MmParser.parse(root)
      act.toList must be (exp)
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
