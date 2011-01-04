package com.tngtech.mmtaskspdfprinter.parsing

import com.tngtech.mmtaskspdfprinter.scrum._
import org.apache.commons.lang.StringEscapeUtils
import scala.xml._

object MmParser {
  private val storyAnnotation = "bookmark"
  private val taskAnnotation = "attach"
  private val backlogPattern = """(?i)\s*(.*Sprint.*|.*Backlog.*)\s*""".r
  private val pointsExtractor = """.*[\(\{](.*=)?\s*(\d+).*[\)\}].*""".r

  def parse(root: Elem): Seq[SprintBacklog] =
    if (sanityCheck(root))
      traverseBacklogs(root\"node" head)
    else
      throw new ParsingException("Provided XML data is not a valid mm-file.")

  private def sanityCheck(root: Elem) = root.label == "map" && root.size == 1

  private def traverseBacklogs(root: Node) = 
    (root\"node") flatMap {possibleBacklogNode =>
      (possibleBacklogNode\"@TEXT").head.text match {
        case backlogPattern(name) =>
          val stories = traverseStories(possibleBacklogNode)
          val backlog = SprintBacklog(extractDescription(name), stories: _*)
          Seq(backlog)
        case _ =>  Nil
      }
    }

  private def traverseStories(backlogNode: Node): Seq[Story] = {
    val pathsToStories = (backlogNode\"node") flatMap {sprintNode => 
      findIcon(List(sprintNode), storyAnnotation)
    }

    pathsToStories.zipWithIndex map {case (path, prio) =>
      val desc = extractDescription(path.head)
      val points = extractScrumPoints(path.head)
      val tasks = traverseTasks(path.head)
      Story(desc, points, prio + 1, tasks: _*)
   }
  }

  private def findIcon(path: List[Node], lookedFor: String): Seq[Seq[Node]] =
    if ( path.head\"icon" exists {icon => (icon\"@BUILTIN").head.text == lookedFor} )
      Seq(path)
    else
      (path.head)\"node" flatMap {child => findIcon(child :: path, lookedFor)}

  private def traverseTasks(sprintNode: Node): Seq[Task] = {
    val pathsToTasks = sprintNode\"node" flatMap {taskNode => 
      findIcon(List(taskNode), taskAnnotation)
    }

    pathsToTasks map {path => 
      val cat = path.tail.map {node => 
        extractDescription(node)
      }.reverse.mkString(" ")
      val desc = extractDescription(path.head)
      val subtasks = traverseSubtasks(path.head)
      Task(desc, cat, subtasks: _*)
    }
  }

  private def traverseSubtasks(taskNode: Node): List[Subtask] = {
    val pathsToLeaves = (taskNode\"node").flatMap {subtaskRoot =>
      findLeaves(List(subtaskRoot))
    }.toList

    pathsToLeaves map {path =>
      val desc = path.map {extractDescription}.reverse.mkString(" ")
      Subtask(desc)
    }
  }

  private def findLeaves(path: Seq[Node]): Seq[Seq[Node]] =
    if ( (path.head\"node").isEmpty )
      Seq(path)
    else
      (path.head\"node") flatMap {child => findLeaves(child +: path)}

  private def extractScrumPoints(node: Node): Option[Int] =
    (node\"@TEXT").head.text match {
      case pointsExtractor(_, points) => Some(Integer.parseInt(points))
      case _ => None
    }

  private def extractDescription(node: Node): String = 
    extractDescription((node\"@TEXT").head.text)
  
  private def extractDescription(text: String): String = {
    var result = text.replaceAll("""\(\s*\d+.*\)""", "") // Things in brackets
    result = result.replaceAll("""\{\s*\d+.*\}""", "") // Things in curly brackets
    result = StringEscapeUtils.unescapeHtml(result)
    result = result.replaceAll("""^\s+""", "")
    result = result.replaceAll("""\s+$""", "")
    result = result.replaceAll("""\s+""", " ")
    result    
  }
}
