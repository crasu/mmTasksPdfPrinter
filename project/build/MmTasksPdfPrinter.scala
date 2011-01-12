import sbt._

import java.io.File

class MmTasksPdfPrinter(info: ProjectInfo) extends DefaultWebProject(info) {

  val suffix = ".war"
  override lazy val jarPath: Path = outputPath / (defaultJarBaseName + suffix)
  val shrinkedJarName = defaultJarBaseName + "Shrinked" + suffix
  lazy val shrinkedJarPath: Path = outputPath / shrinkedJarName
	val proguardConfigurationPath: Path = outputPath / "proguard.pro"

  val toolsConfig = config("tools")
  val proguardLibsConfig = config("proguard")

  val iTextUrl = new java.net.URL("http://maven.itextpdf.com/")
  val iTextRepo = Resolver.url("com.itextpdf", iTextUrl)

  val lift = "net.liftweb" %% "lift-mapper" % "2.1" % "compile->default"
  val commons = "commons-lang" % "commons-lang" % "2.4" % "compile->default"
  val itext = "com.itextpdf" % "itextpdf" % "5.0.2" % "compile->default"
  val servlet = "javax.servlet" % "servlet-api" % "2.3" % "proguard->default"
  /*
   * xmlrpcClient dependencies needed to be resolved manually since:
   * xmlrpcClient depends on xmLrpcCommon which depends on wsCommons which depends on xml-apis
   * xml-apis may not be added as dependency since it brings jce.jar would then
   * also depend on xml-apis and this would cause proguard to fail
   */
  val xmlrpcClient = "org.apache.xmlrpc" % "xmlrpc-client" % "3.1.3" % "compile->default" intransitive
  val xmlrpcCommon = "org.apache.xmlrpc" % "xmlrpc-common" % "3.1.3" % "compile->default" intransitive
  val wsCommons = "org.apache.ws.commons" % "ws-commons-util" % "1.0.1" % "compile->default" intransitive
  val jettyDep =  "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default"
  val junit = "junit" % "junit" % "4.5" % "test->default"
  val scalatest = "org.scalatest" % "scalatest" % "1.2" % "test->default"
  val proguardDep = "net.sf.proguard" % "proguard" % "4.4" % "tools->default"
  val container = "org.jvnet.hudson.winstone" % "winstone" % "0.9.10-hudson-24" % "tools->default"

  override lazy val `package` = packageAction dependsOn(createClassesJar, removeClasses)
  lazy val createClassesJar = createClassesJarTask(temporaryWarPath) dependsOn(prepareWebapp) describedAs ("Creating classes.jar")
  lazy val removeClasses = removeClassesTask(temporaryWarPath) dependsOn(prepareWebapp, createClassesJar) describedAs ("Removes class files in WEB-INF/classes")
  lazy val proguardConfig = writeProguardConfigurationTask describedAs("Creating proguard config")
  lazy val fetchContainer = fetchContainerTask dependsOn() describedAs("Adds a servlet container to target dir")
  lazy val writeLauncherScript = writeLauncherScriptTask("startMmPrinter.sh", container.name+"-"+container.revision+".jar", shrinkedJarName)  dependsOn(fetchContainer) describedAs("Writes a small start script")
  lazy val proguard = proguardTask dependsOn(`package`, proguardConfig, writeLauncherScript) describedAs("Reduces size of packaged war using ProGuard")

  private def createClassesJarTask(warPath: => Path) = task {
    val webInfPath = warPath / "WEB-INF"
    val webLibDirectory = webInfPath / "lib"
    val classesTargetDirectory = webInfPath / "classes"

    FileUtilities.clean(webLibDirectory / "classes.jar" :: Nil, log)
    FileUtilities.zip(descendents(classesTargetDirectory ##, "*.class").get, webLibDirectory / "classes.jar", false, log)
  }

  private def removeClassesTask(warPath: => Path) = task {
    val webInfPath = warPath / "WEB-INF"
    val classesTargetDirectory = webInfPath / "classes"
    FileUtilities.clean(classesTargetDirectory / "com" :: classesTargetDirectory / "bootstrap":: Nil, log)
    None 
  }

	private def writeProguardConfigurationTask = task {
    val outTemplate = """
      |-dontoptimize
      |-dontobfuscate
      |-injars %s
      |-outjars %s
      |-libraryjars <java.home>/lib/rt.jar 
      |-libraryjars <java.home>/lib/jce.jar
      |-libraryjars %s
      |-dontskipnonpubliclibraryclasses
      |-dontskipnonpubliclibraryclassmembers
      |-dontnote
      |-ignorewarnings
      |-dontwarn **$$anonfun$*
      |-dontwarn scala.collection.immutable.RedBlack$Empty
      |-dontwarn scala.tools.**,plugintemplate.**
      |
      |-keep class * implements org.xml.sax.EntityResolver
      |
      |-keepclassmembers class * {
      |    ** MODULE$;
      |}
      |-keep public class bootstrap.liftweb.Boot {
      | public *;
      |}
      |-keep public class com.tngtech.** {
      | public *;
      |}
      |-keep public class * extends javax.servlet.** {
      | public *;
      |}
      |-keep public class * extends org.apache.log4j.Appender {
      | public *;
      |}
      |-keep public class * extends org.apache.log4j.Layout {
      | public *;
      |}
      |"""
      val libClasspath = managedClasspath(proguardLibsConfig)
      val libClasspathString = Path.makeString(libClasspath.get)
      val proguardConfiguration =
        outTemplate.stripMargin.format(
          mkpath(jarPath.asFile),
          mkpath(shrinkedJarPath.asFile),
          libClasspathString)
			FileUtilities.write(proguardConfigurationPath.asFile, proguardConfiguration, log)
      None
  }

  private def proguardTask = task {
    FileUtilities.clean(shrinkedJarPath :: Nil, log)
    val proguardClasspath = managedClasspath(toolsConfig)
    val proguardClasspathString = Path.makeString(proguardClasspath.get)
    val configFile = proguardConfigurationPath.asFile.getAbsolutePath
    val exitValue = Process("java", List("-Xmx512M", "-cp", proguardClasspathString, "proguard.ProGuard", "@" + configFile)) ! log 
    if (exitValue == 0) None else Some("Proguard failed with nonzero exit code (" + exitValue + ")")
  }

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
