import sbt._

import java.io.File

class MmTasksPdfPrinter(info: ProjectInfo) extends DefaultWebProject(info) with IdeaProject {

  val suffix = ".war"
  override lazy val jarPath: Path = outputPath / (defaultJarBaseName + suffix)
  val jarName = defaultJarBaseName + suffix

  val toolsConfig = config("tools")

  val iTextUrl = new java.net.URL("http://maven.itextpdf.com/")
  val iTextRepo = Resolver.url("com.itextpdf", iTextUrl)

  val lift = "net.liftweb" %% "lift-mapper" % "2.1" % "compile->default"
  val commons = "commons-lang" % "commons-lang" % "2.4" % "compile->default"
  val itext = "com.itextpdf" % "itextpdf" % "5.0.2" % "compile->default"
  val xmlrpcClient = "org.apache.xmlrpc" % "xmlrpc-client" % "3.1.3" % "compile->default"
  val jerichoHtmlParser = "net.htmlparser.jericho" % "jericho-html" % "3.1" % "compile->default"
  val httpClient = "commons-httpclient" % "commons-httpclient" % "3.1" % "compile->default"
  val jettyDep =  "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default"
  val junit = "junit" % "junit" % "4.5" % "test->default"
  val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test->default"
  val container = "org.jvnet.hudson.winstone" % "winstone" % "0.9.10-hudson-24" % "tools->default"

  lazy val fetchContainer = fetchContainerTask dependsOn() describedAs("Adds a servlet container to target dir")
  lazy val writeLauncherScript = writeLauncherScriptTask("startMmPrinter.sh", container.name+"-"+container.revision+".jar", jarName)  dependsOn(fetchContainer) describedAs("Writes a small start script")
  override lazy val `package` = packageAction dependsOn(writeLauncherScript)

private def writeLauncherScriptTask(scriptName: String, warName: String, containerName: String) = task {
  val scriptTemplate = """
  |#!/bin/bash
  |java -jar %s --warfile=%s --httpPort=8081
  """
  val script = scriptTemplate.stripMargin.format(
    warName,
    containerName)
  val scriptFile = (outputPath / scriptName).asFile
  FileUtilities.write(scriptFile, script, log)
  scriptFile.setExecutable(true)
  None
}

	private def mkpath(f: File) = '\"' + f.getAbsolutePath + '\"'

  private def fetchContainerTask = task {
    val containerPath = managedDependencyPath / toolsConfig.toString / (container.name+"-"+container.revision+".jar")
    FileUtilities.copyFlat(containerPath.get, outputPath, log).left.toOption
  }
}
