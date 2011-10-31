package com.tngtech.mmtaskspdfprinter.parsing

import com.tngtech.mmtaskspdfprinter.scrum._

import org.apache.commons.lang.StringEscapeUtils
import scala.xml._
import net.htmlparser.jericho._

object MmParser {
  private val storyAnnotation = "bookmark"
  private val taskAnnotation = "attach"
  private val backlogPattern = """(?i)\s*(.*Sprint.*|.*Backlog.*)\s*""".r
  private val pointsExtractor = """.*[\(\{](.*=)?\s*(\d+).*[\)\}].*""".r
  private val halfPointsExtractor = """.*[\(\{](.*=)?\s*(0[\.,]5).*[\)\}].*""".r

  def parse(root: Elem): Seq[SprintBacklog] =
    if (sanityCheck(root))
      traverseBacklogs(root\"node" head)
    else
      throw new ParsingException("Provided XML data is not a valid mm-file.")

  def sanityCheck(root:  Elem) = root.label == "map" && root.size == 1

  def traverseBacklogs(root: Node) =    
    (root\"node") flatMap {possibleBacklogNode =>
      extractText(possibleBacklogNode) match {
        case backlogPattern(name) =>
          val stories = extractStoriesFromSprint(possibleBacklogNode)
          val backlog = SprintBacklog(extractDescription(name), stories: _*)
          Seq(backlog)
        case _ =>  Nil
      }
    }

  def hasIcon(node:Node, iconName:String) = 
    node \ "icon" exists (icon => (icon\"@BUILTIN").head.text == iconName)
    
  def extractStoriesFromSprint(backlogNode: Node): Seq[Story] = {
    val storyNodes = backlogNode \\ "node" filter (story => hasIcon(story, storyAnnotation))
    storyNodes.zipWithIndex map {case (story, prio) =>
      val desc = extractDescription(story)
      val points = extractScrumPoints(story) 
      val tasks = extractTasksFromStory(story)
      val acceptance = extractAcceptanceCriteria(story)
      Story(desc, points, Some(prio + 1), tasks, acceptance)
   }
  }
  
  def extractAcceptanceCriteria(story:Node) =
    story \\ "node" filter (hasIcon(_, "list")) flatMap (_ \ "node" map extractDescription)

  def extractTasksFromStory(sprintNode: Node): Seq[Task] = {
    def loop(node: Node, categories: List[String]):Seq[Task] =
      if (hasIcon(node, taskAnnotation)) {
        val desc = extractDescription(node)
        val subtasks = extractSubtasks(node)
        val cat = categories.reverse mkString " "
        Seq(Task(desc, cat, subtasks))
      } else {
        val cat = extractDescription(node) :: categories
        node \ "node" flatMap (loop(_, cat))
      }
    
    sprintNode \ "node" flatMap (loop(_, Nil))
  }
  
  def extractSubtasks(taskNode: Node): List[Subtask] = {
    def findLeaves(path: Seq[Node]): Seq[Seq[Node]] =
      if ((path.head \ "node").isEmpty)
        Seq(path)
      else
        (path.head \ "node") flatMap { child => findLeaves(child +: path) }
    
    val pathsToLeaves = (taskNode\"node").flatMap {subtaskRoot =>
      findLeaves(List(subtaskRoot))
    }.toList

    pathsToLeaves map {path =>
      val desc = path.map {extractDescription}.reverse.mkString(" ")
      Subtask(desc)
    }
  }

  private def extractScrumPoints(node: Node): ScrumPoints =
    extractText(node) match {
      case halfPointsExtractor(_, _) => HalfScrumPoint
      case pointsExtractor(_, points) => IntScrumPoints(Integer.parseInt(points))
      case _ => UndefScrumPoints
    }

  private def extractDescription(node: Node): String =
    extractDescription(extractText(node))
  
  private def extractDescription(text: String): String = {
    var result = text.replaceAll("""\(\s*\d+.*\)""", "") // Things in brackets
    result = result.replaceAll("""\{\s*\d+.*\}""", "") // Things in curly brackets
    result = StringEscapeUtils.unescapeHtml(result)
    result = result.replaceAll("""^\s+""", "")
    result = result.replaceAll("""\s+$""", "")
    result = result.replaceAll("""\s+""", " ")
    result    
  }

  private def extractText(node: Node): String =
    if ((node\"@TEXT").size > 0) (node\"@TEXT").head.text
    else if ((node\"richcontent").size > 0) extractTextFromHtml((node\"richcontent").head.text)
    else ""


  private def extractTextFromHtml(html: String): String = {
    val source = new Source(html)
    source.getTextExtractor().toString()
  }
}
