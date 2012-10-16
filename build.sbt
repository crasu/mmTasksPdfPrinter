import AssemblyKeys._ 

name := "mmTasksPdfPrinter"

version := "1.4"

organization := "com.tngtech"

// set the Scala version used for the project
scalaVersion := "2.9.2"

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"

libraryDependencies ++= Seq(
  	"net.liftweb" % "lift-mapper_2.9.1" % "2.4-M4" % "compile->default",
	"commons-lang" % "commons-lang" % "2.6" % "compile->default",
	"com.itextpdf" % "itextpdf" % "5.3.2" % "compile->default",
	"org.mortbay.jetty" % "jetty" % "6.1.26" % "container",
  	"net.sf.jopt-simple" % "jopt-simple" % "4.3" % "compile->default" withSources,
  	"net.htmlparser.jericho" % "jericho-html" % "3.2" % "compile->default",
	"commons-httpclient" % "commons-httpclient" % "3.1" % "compile->default"
)

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.10" % "test->default",
  "org.scalatest" %% "scalatest" % "1.8" % "test->default",
  "org.specs2" % "specs2_2.9.1" % "1.8.1" % "test->default",
  "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test->default",
  "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test->default",
  "org.mockito" % "mockito-core" % "1.9.0" % "test->default",
  "org.hamcrest" % "hamcrest-library" % "1.3" % "test->default",
  "com.novocode" % "junit-interface" % "0.8" % "test->default"
)

seq(com.github.siasia.WebPlugin.webSettings :_*)

// reduce the maximum number of errors shown by the Scala compiler
maxErrors := 20

// increase the time between polling for file changes when using continuous execution
pollInterval := 1000

// append -deprecation to the options passed to the Scala compiler
scalacOptions += "-deprecation"

// define the statements initially evaluated when entering 'console', 'console-quick', or 'console-project'
initialCommands := """
  import System.{currentTimeMillis => now}
  def time[T](f: => T): T = {
    val start = now
    try { f } finally { println("Elapsed: " + (now - start)/1000.0 + " s") }
  }
"""
  
// disable updating dynamic revisions (including -SNAPSHOT versions)
offline := true

// set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

// set the prompt (for the current project) to include the username
shellPrompt := { state => System.getProperty("user.name") + "> " }

// change the format used for printing task completion time
timingFormat := {
	import java.text.DateFormat
	DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
}

// disable using the Scala version in output paths and artifacts
crossPaths := false

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx2G -XX:MaxPermSize=512M"

// only store messages at info and above (the default is Debug)
//   this is the logging level for replaying logging with 'last'
persistLogLevel := Level.Debug

// Copy all managed dependencies to <build-root>/lib_managed/
//   This is essentially a project-local cache and is different
//   from the lib_managed/ in sbt 0.7.x.  There is only one
//   lib_managed/ in the build root (not per-project).
retrieveManaged := true

assemblySettings
