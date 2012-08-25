name := "mmTasksPdfPrinter"

version := "1.4"

organization := "com.tngtech"

// set the Scala version used for the project
scalaVersion := "2.9.1"

// set the main Scala source directory to be <base>/src
//scalaSource in Compile <<= baseDirectory(_ / "src")

// set the Scala test source directory to be <base>/test
//scalaSource in Test <<= baseDirectory(_ / "test")

resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"

libraryDependencies ++= Seq(
    "net.liftweb" %% "lift-mapper" % "2.4-M4" % "compile->default",
	"commons-lang" % "commons-lang" % "2.4" % "compile->default",
    "com.itextpdf" % "itextpdf" % "5.1.2" % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "container->default",
    "net.sf.jopt-simple" % "jopt-simple" % "4.1" % "compile->default" withSources,
    "net.htmlparser.jericho" % "jericho-html" % "3.1" % "compile->default",
    "commons-httpclient" % "commons-httpclient" % "3.1" % "compile->default"
)

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.5" % "test->default",
    "org.scalatest" %% "scalatest" % "1.6.1" % "test->default",
    "org.specs2" %% "specs2" % "1.6.1" % "test->default",
    "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test->default",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test->default",
    "org.mockito" % "mockito-core" % "1.8.4" % "test->default",
    "org.hamcrest" % "hamcrest-library" % "1.2.1" % "test->default",
    "com.novocode" % "junit-interface" % "0.7" % "test->default"
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
  
// set the initial commands when entering 'console' only
// initialCommands in console := "import myproject._"

// set the main class for packaging the main jar
// 'run' will still auto-detect and prompt
// change Compile to Test to set it for the test jar
// mainClass in (Compile, packageBin) := Some("myproject.MyMain")

// set the main class for the main 'run' task
// change Compile to Test to set it for 'test:run'
// mainClass in (Compile, run) := Some("myproject.MyMain")

// add <base>/input to the files that '~' triggers on
// watchSources <+= baseDirectory map { _ / "input" }

// set Ivy logging to be at the highest level
// ivyLoggingLevel := UpdateLogging.Full

// disable updating dynamic revisions (including -SNAPSHOT versions)
offline := true

// set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

// set the prompt (for the current project) to include the username
shellPrompt := { state => System.getProperty("user.name") + "> " }

// disable printing timing information, but still print [success]
//showTiming := false

// change the format used for printing task completion time
timingFormat := {
	import java.text.DateFormat
	DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
}

// disable using the Scala version in output paths and artifacts
crossPaths := false

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx2G -XX:MaxPermSize=512M"

// only use a single thread for building
//parallelExecution := false

// Execute tests in the current project serially
//   Tests from other projects may still run concurrently.
//parallelExecution in Test := false

// set the location of the JDK to use for compiling Java code.
// if 'fork' is true, this is used for 'run' as well
//javaHome := Some(file("/usr/lib/jvm/sun-jdk-1.6"))

// Use Scala from a directory on the filesystem instead of retrieving from a repository
//scalaHome := Some(file("/home/user/scala/trunk/"))

// don't aggregate clean (See FullConfiguration for aggregation details)
//aggregate in clean := false

// only show warnings and errors on the screen for compilations.
//  this applies to both test:compile and compile and is Info by default
//logLevel in compile := Level.Warn

// only show warnings and errors on the screen for all tasks (the default is Info)
//  individual tasks can then be more verbose using the previous setting
//logLevel := Level.Warn

// only store messages at info and above (the default is Debug)
//   this is the logging level for replaying logging with 'last'
persistLogLevel := Level.Debug

// only show 10 lines of stack traces
//traceLevel := 10

// only show stack traces up to the first sbt stack frame
//traceLevel := 0

// add SWT to the unmanaged classpath
//unmanagedJars in Compile += Attributed.blank(file("/usr/share/java/swt.jar"))

// Copy all managed dependencies to <build-root>/lib_managed/
//   This is essentially a project-local cache and is different
//   from the lib_managed/ in sbt 0.7.x.  There is only one
//   lib_managed/ in the build root (not per-project).
retrieveManaged := true

