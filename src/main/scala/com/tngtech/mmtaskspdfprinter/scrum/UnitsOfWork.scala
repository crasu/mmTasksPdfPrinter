package com.tngtech.mmtaskspdfprinter.scrum
import java.io._

case class SprintBacklog(val name: String, val stories: Story*)

object Story {
  def apply(name: String, scrumPoints: Int, priority: Int, tasks: Task*): Story =
    Story(name, IntScrumPoints(scrumPoints), Some(priority), tasks: _*)
  def apply(name: String, scrumPoints: ScrumPoints, priority: Int, tasks: Task*): Story =
    Story(name, scrumPoints, Some(priority), tasks: _*)
  def apply(name: String, scrumPoints: Int, priority: Option[Int], tasks: Task*): Story =
    Story(name, IntScrumPoints(scrumPoints), priority, tasks: _*)
}

trait JiraUnitOfWork {
  var jiraKey: String = ""
}

abstract class ScrumPoints() {
  def toString(default:String):String
}

case class IntScrumPoints(p: Int) extends ScrumPoints {
  override def toString(default:String) = p.toString
}

case object HalfScrumPoint extends ScrumPoints {
  override def toString(default:String) = "0.5"
}

case object UndefScrumPoints extends ScrumPoints {
  override def toString(default:String) = default
}

case class Story(val name: String,
  val scrumPoints: ScrumPoints,
  val priority: Option[Int],
  val tasks: Task*) extends JiraUnitOfWork

case class Task(val description: String,
  val category: String,
  val subtasks: Subtask*) extends JiraUnitOfWork

case class Subtask(val description: String) extends JiraUnitOfWork