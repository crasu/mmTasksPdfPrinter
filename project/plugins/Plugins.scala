import sbt._
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.2.0"
  lazy val eclipse = "de.element34" % "sbt-eclipsify" % "0.7.0"
  val repo = "Christoph's Maven Repo" at "http://maven.henkelmann.eu/"
  val junitXml = "eu.henkelmann" % "junit_xml_listener" % "0.2"
}
