package com.tngtech.mmtaskspdfprinter.parsing

import com.tngtech.mmtaskspdfprinter.scrum._
import org.apache.commons.lang.StringEscapeUtils
import scala.xml._

object MmParser {
  private val STORY_ANNOTATION = "bookmark"
  private val TASK_ANNOTATION = "attach"
  private val SPRINT_PATTERN = """\s*Sprint\s+(\d{4}-\d+).*""".r

  def parse(root: Elem): Seq[SprintBacklog] = {
    if (!sanityCheck(root)) {
      throw new ParsingException("Provided XML data is not a valid mm-file.")
    }

    traverseBacklogs(root\"node" first)
  }

  private def sanityCheck(root: Elem) = {
    root.label == "map" &&
    root.size == 1
  }

  private def traverseBacklogs(root: Node) = {
    var backlogs = (root\"node").flatMap(possibleBacklogNode => {
        (possibleBacklogNode\"@TEXT").toString() match {
          case SPRINT_PATTERN(name) => {
              val backlog = SprintBacklog(name)
              backlog.stories = traverseStories(possibleBacklogNode)
              List(backlog)
          }
          case _ =>  List()
        }
      })
    backlogs
  }

  private def traverseStories(backlogNode: Node): List[Story] = {
    val pathsToStories = (backlogNode\"node").flatMap( sprintNode => {
        findIcon(List(sprintNode), STORY_ANNOTATION)
      }).toList

    var priority = 1
    pathsToStories.map(path => {
        val desc = extractDescription(path.head)
        val points = extractScrumPoints(path.head)
        val story = Story(desc, points, priority)
        priority += 1
        story.tasks = traverseTasks(path.head)
        story
      }).toList
  }

  private def findIcon(path: List[Node], lookedFor: String): List[List[Node]] = {
    if ( ((path.head)\"icon").exists(icon => (icon\"@BUILTIN").toString() == lookedFor) ) {
      return List(path)
    }
    ((path.head)\"node").flatMap(child => findIcon(child :: path, lookedFor)).toList
  }

  private def traverseTasks(sprintNode: Node): List[Task] = {
    val pathsToTasks = (sprintNode\"node").flatMap( taskNode => {
        findIcon(List(taskNode), TASK_ANNOTATION)
      }).toList

    pathsToTasks.map(path => {
        val cat = path.tail.map( node => extractDescription(node) ).reverse.mkString(" ")
        val desc = extractDescription(path.head)
        val task = Task(desc, cat)
        task.subtasks = traverseSubtasks(path.head).toList
        task
      }).toList
  }

  private def traverseSubtasks(taskNode: Node): List[Subtask] = {
    val pathsToLeaves = (taskNode\"node").flatMap( subtaskRoot => {
        findLeaves(List(subtaskRoot))
      }).toList

    pathsToLeaves.map(path => {
        val desc = path.map( node => extractDescription(node) ).reverse.mkString(" ")
        Subtask(desc)
      }).toList
  }

  private def findLeaves(path: List[Node]): List[List[Node]] = {
    if (((path.head)\"node").isEmpty) {
      return List(path)
    }
    ((path.head)\"node").flatMap(child => findLeaves(child :: path)).toList
  }

  private def extractScrumPoints(node: Node): Int = {
    val text = (node\"@TEXT").toString
    var pointsExtractor = """.*[\(\{](.*=)?\s*(\d+).*[\)\}].*""".r
    text match {
      case pointsExtractor(_, points) => Integer.parseInt(points)
      case _ => Story.NO_ESTIMATION
    }
  }

  private def extractDescription(node: Node) = {
    val text = (node\"@TEXT").toString
    var result = text.replaceAll("""\(\s*\d+.*\)""", "") // Things in brackets
    result = result.replaceAll("""\{\s*\d+.*\}""", "") // Things in curly brackets
    result = StringEscapeUtils.unescapeHtml(result)
    result = result.replaceAll("""^\s+""", "")
    result = result.replaceAll("""\s+$""", "")
    result = result.replaceAll("""\s+""", " ")
    result
  }
}
