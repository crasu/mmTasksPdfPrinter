package com.tngtech.mmtaskspdfprinter.scrum

object Dsl {
	implicit def intToPoints(n: Int) = new PointBuilder(n)
  implicit def intToPriority(n: Int) = new PrioBuilder(n)
  
  final class PointBuilder(n: Int) {
	  def point = IntScrumPoints(n)
	  def points = point
	  def pts = point
	  def pt = point
	}
	
	final class PrioBuilder(n: Int) {
	  def prio = Some(n)
	  def priority = prio
	  
	  /**
	   * Support for a syntax like Story(.., prioriy = 1 st)
	   */
	  def st = prio
	  def nd = prio
	  def rd = prio
	  def th = prio
	}
	
	def NoPrio = None
}