package com.tngtech.mmtaskspdfprinter.scrum
import java.io._

case class SprintBacklog(val name: String) {
  var stories = List[Story]()
  override def equals(that: Any) = that match {
      case other: SprintBacklog => other.name == name &&
                      other.stories == stories
      case _ => false
    }
  override def hashCode() = 41*(name.hashCode)+stories.hashCode
  override def toString() = "SprintBacklog(Name: " + name + "; " + stories.mkString(", ") + ")"
}

object Story {
  val NO_ESTIMATION = -1
  val NO_PRIORITY = -1
  def apply(name: String) = {
    new Story(name, NO_ESTIMATION, NO_PRIORITY)
  }
  def apply(name: String, scrumPoints: Int) = {
    new Story(name, scrumPoints, NO_PRIORITY)
  }
}

case class Story(val name: String, val scrumPoints: Int, val priority: Int) {
  var tasks = List[Task]()
  override def equals(that: Any) = that match {
      case other: Story => other.name == name &&
                      other.scrumPoints == scrumPoints &&
                      other.priority == priority &&
                      other.tasks == tasks
      case _ => false
    }
  override def hashCode() = 41*((41*name.hashCode) + scrumPoints.hashCode)+tasks.hashCode
  override def toString() = "Story(Name: " + name + ", Points: " + scrumPoints + ", Priority: " + priority + "; " + tasks.mkString(", ") + ")"
}

case class Task(val description: String, val category: String) {
  var subtasks = List[Subtask]()
  override def equals(that: Any) = that match {
      case other: Task => other.description == description &&
                      other.category == category &&
                      other.subtasks == subtasks
      case _ => false
    }
  override def hashCode() = 41*((41*description.hashCode) + category.hashCode)+subtasks.hashCode
  override def toString() = "Task(Desc: " + description + ", Cat: " + category + "; " + subtasks.mkString(", ") + ")"
}
case class Subtask(val description: String) extends Serializable