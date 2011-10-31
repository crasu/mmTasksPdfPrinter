package com.tngtech.mmtaskspdfprinter.scrum
import java.io._

case class SprintBacklog(name: String, stories: Story*)

object Story {
  def apply(name: String, scrumPoints: Int, priority: Int, tasks: Seq[Task]): Story =
    Story(name, IntScrumPoints(scrumPoints), Some(priority), tasks)
    
  def apply(name: String, scrumPoints: Int, priority: Int): Story =
    Story(name, IntScrumPoints(scrumPoints), Some(priority), Nil)
    
  def apply(name: String, scrumPoints: ScrumPoints, priority: Int, tasks: Seq[Task]): Story =
    Story(name, scrumPoints, Some(priority), tasks)
    
  def apply(name: String, scrumPoints: ScrumPoints, priority: Int): Story =
    Story(name, scrumPoints, Some(priority), Nil)
    
  def apply(name: String, scrumPoints: ScrumPoints, priority: Option[Int]): Story =
    Story(name, scrumPoints, priority, Nil)
    
  def apply(name: String, scrumPoints: ScrumPoints): Story =
    Story(name, scrumPoints, None, Nil)
    
  def apply(name: String, scrumPoints: Int, priority: Option[Int], tasks: Seq[Task]): Story =
    Story(name, IntScrumPoints(scrumPoints), priority, tasks)
    
  def apply(name: String, scrumPoints: Int, priority: Option[Int]): Story =
    Story(name, IntScrumPoints(scrumPoints), priority, Nil)
    
  def apply(name: String, scrumPoints: ScrumPoints, priority: Option[Int], tasks: Seq[Task]): Story =
    	Story(name, scrumPoints, priority, tasks, Nil)
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

case class Story(name: String,
  scrumPoints: ScrumPoints,
  priority: Option[Int],
  tasks: Seq[Task],
  acceptanceCriteria: Seq[String]) extends JiraUnitOfWork {
}

case class Task(description: String,
  category: String,
  subtasks: Seq[Subtask]) extends JiraUnitOfWork {
  if (!subtasks.isInstanceOf[List[_]])
    new RuntimeException(subtasks.getClass().toString()).printStackTrace()
}
  
object Task {
  def apply(description: String, category: String) :Task
  	= Task(description, category, Nil)
}

case class Subtask(val description: String) extends JiraUnitOfWork
