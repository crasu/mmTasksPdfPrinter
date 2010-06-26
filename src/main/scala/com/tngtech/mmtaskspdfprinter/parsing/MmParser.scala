package com.tngtech.mmtaskspdfprinter.parsing

import com.tngtech.mmtaskspdfprinter.scrum._
import org.apache.commons.lang.StringEscapeUtils
import scala.xml._

object MmParser {
  private val taskAnnotation = "attach"
  private val sprintPattern = """\s*Sprint\s+(\d{4}-\d+).*""".r

  def parse(root: Elem): Seq[SprintBacklog] = {
    if (!sanityCheck(root)) {
      throw new ParsingException("Provided XML data is not a valid mm-file.")
    }

    traverseBacklog(root\"node" first)
  }

  private def sanityCheck(root: Elem) = {
    root.label == "map" &&
    root.size == 1
  }

  private def traverseBacklog(root: Node) = {
    var backlogs = List[SprintBacklog]()
    (root\"node").foreach(possibleSprintNode => {
        (possibleSprintNode\"@TEXT").toString() match {
          case sprintPattern(name) =>
             backlogs += traverseSprint(possibleSprintNode, name)
          case _ =>  ()
        }
      })
    backlogs
  }

  private def traverseSprint(sprintNode: Node, name: String) = {
    val sprintBacklog = SprintBacklog(name)
    var priority = 0
    sprintBacklog.stories =
      ((sprintNode\"node").map(storyNode => {
          priority += 1
          traverseStory(storyNode, priority)
        })
      ).toList
    sprintBacklog
  }

  private def traverseStory(storyRoot: Node, priority: Int) = {
    val story = Story(extractDescription((storyRoot\"@TEXT").toString()),
                      extractScrumPoints((storyRoot\"@TEXT").toString()),
                      priority)
    story.tasks =
      ((storyRoot\"node").flatMap(elem =>
        traverseCategories("", elem))
      ).toList
    story
  }

  private def traverseCategories(category: String, node: Node): Seq[Task] = {
    if (isTask(node)) {
     List(extractTask(category, node))
    }
    else {
      var extendedCat = ""
      val currentCat = extractDescription((node\"@TEXT").toString())
      if(category.isEmpty) {
        extendedCat = currentCat
      }
      else {
        extendedCat = category + " " + currentCat
      }
      (node\"node").flatMap(child => traverseCategories(extendedCat, child))
    }
  }

  private def isTask(node: Node): Boolean = {
    (node\"icon").exists(icon => (icon\"@BUILTIN").toString() == taskAnnotation)
  }

  private def extractTask(category: String, taskRoot: Node): Task = {
    val task = Task(extractDescription((taskRoot\"@TEXT").toString()),
                    category)
    task.subtasks =
      ((taskRoot\"node").flatMap(possibleSubTask =>
        traverseSubtasks("", possibleSubTask))
      ).toList
    task
  }

  private def traverseSubtasks(desc: String, subtaskNode: Node): Seq[Subtask] = {
    var extDesc = ""
    if(desc isEmpty) {
      extDesc = (subtaskNode\"@TEXT").toString
    }
    else {
      extDesc = desc + " " + (subtaskNode\"@TEXT").toString
    }

    if((subtaskNode\"node").isEmpty) {
      List(Subtask(extDesc))
    }
    else {
      (subtaskNode\"node").flatMap (child => traverseSubtasks(extDesc, child))
    }
  }

  private def extractScrumPoints(text: String): Int = {
    var pointsExtractor = """.*[\(\{](.*=)?\s*(\d+).*[\)\}].*""".r
    text match {
      case pointsExtractor(_, points) => Integer.parseInt(points)
      case _ => Story.NO_ESTIMATION
    }
  }

  private def extractDescription(text: String) = {
    var result = text.replaceAll("""\(\s*\d+.*\)""", "") // Things in brackets
    result = result.replaceAll("""\{\s*\d+.*\}""", "") // Things in curly brackets
    result = StringEscapeUtils.unescapeHtml(result)
    result = result.replaceAll("""^\s+""", "")
    result = result.replaceAll("""\s+$""", "")
    result = result.replaceAll("""\s+""", " ")
    result
  }
}
