package com.tngtech.mmtaskspdfprinter.scrum
import java.io._

case class SprintBacklog(val name: String, val stories: Story*)

object Story {
  def apply(name: String, scrumPoints: Int, priority: Int, tasks: Task*): Story =
    Story(name, Some(scrumPoints), Some(priority), tasks: _*)
  def apply(name: String, scrumPoints: Option[Int], priority: Int, tasks: Task*): Story =
    Story(name, scrumPoints, Some(priority), tasks: _*)
  def apply(name: String, scrumPoints: Int, priority: Option[Int], tasks: Task*): Story =
    Story(name, Some(scrumPoints), priority, tasks: _*)
}

trait JiraUnitOfWork {
  var jiraKey: String = ""
}

case class Story(val name: String, 
                 val scrumPoints: Option[Int], 
                 val priority: Option[Int],
                 val tasks: Task*) extends JiraUnitOfWork

case class Task(val description: String, 
                val category: String,
                val subtasks: Subtask*) extends JiraUnitOfWork

case class Subtask(val description: String) extends JiraUnitOfWork