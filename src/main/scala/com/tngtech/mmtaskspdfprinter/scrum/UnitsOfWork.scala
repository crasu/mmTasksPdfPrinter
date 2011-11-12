package com.tngtech.mmtaskspdfprinter.scrum

sealed trait ScrumPoints {
  def toString(default: String): String
}

case class IntScrumPoints(p: Int) extends ScrumPoints {
  override def toString(default: String) = p.toString
}

case object HalfScrumPoint extends ScrumPoints {
  override def toString(default: String) = "0.5"
}

case object UndefScrumPoints extends ScrumPoints {
  override def toString(default: String) = default
}

object Sprint {
  def apply(name: String, stories: Story*):Sprint = new Sprint(name, stories.toList)
}

case class Sprint(name: String, stories: List[Story])

case class Story(name: String,
	  scrumPoints: ScrumPoints = UndefScrumPoints,
	  priority: Option[Int] = None,
	  tasks: List[Task] = Nil,
	  acceptanceCriteria: Seq[String] = Nil,
	  jiraKey: String = "")

case class Task(description: String,
	  category: String,
	  subtasks: List[Subtask] = Nil,
	  jiraKey: String = "")
  
case class Subtask(description: String, jiraKey: String = "")