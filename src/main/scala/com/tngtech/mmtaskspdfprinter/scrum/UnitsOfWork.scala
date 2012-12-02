package com.tngtech.mmtaskspdfprinter.scrum

import java.security.MessageDigest

trait ScrumPoints {
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
  def apply(name: String, stories: Story*): Sprint = new Sprint(name, stories.toList)
}

case class Sprint(name: String, stories: List[Story])

case class Story(name: String,
  scrumPoints: ScrumPoints = UndefScrumPoints,
  priority: Option[Int] = None,
  tasks: List[Task] = Nil,
  acceptanceCriteria: Seq[String] = Nil,
  key: String = "")

object TaskIdGenerator {
  def generate(plaintext: String): String = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.reset()
    md5.update(plaintext.toCharArray().map(_.toByte))

    md5.digest().map( x => "%c".format((x&0xFF) % 26 + 65) ).slice(1,6).foldLeft("") { _ + _ }
  }  
}

case class Task(description: String,
  category: String,
  subtasks: List[Subtask] = Nil,
  key: String = "") 

case class Subtask(description: String, key: String = "")