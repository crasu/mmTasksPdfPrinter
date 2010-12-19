import sbt._

import java.io.File

class MmTasksPdfPrinter(info: ProjectInfo) extends DefaultWebProject(info) {

  val suffix = ".war"
  override lazy val jarPath: Path = outputPath / (defaultJarBaseName + suffix)
  lazy val proguardJar: Path = outputPath / (defaultJarBaseName + "Shrinked" + suffix)
	val proguardConfigurationPath: Path = outputPath / "proguard.pro"

  val toolsConfig = config("tools")

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-mapper" % "2.1" % "compile->default",
    "commons-lang" % "commons-lang" % "2.4" % "compile->default",
    "com.itextpdf" % "itextpdf" % "5.0.2" % "compile->default",
    "javax.servlet" % "servlet-api" % "2.3" % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "org.scalatest" % "scalatest" % "1.2" % "test->default",
    "net.sf.proguard" % "proguard" % "4.4" % "tools->default"
  ) ++ super.libraryDependencies


  override lazy val `package` = packageAction dependsOn(createClassesJar, removeClasses)
  lazy val createClassesJar = createClassesJarTask(temporaryWarPath) dependsOn(prepareWebapp) describedAs ("Creating classes.jar")
  lazy val removeClasses = removeClassesTask(temporaryWarPath) dependsOn(prepareWebapp, createClassesJar) describedAs ("Removes class files in WEB-INF/classes")
  lazy val proguardConfig = writeProguardConfigurationTask describedAs("Creating proguard config")
  lazy val proguard = proguardTask dependsOn(`package`, proguardConfig) describedAs("Reduces size of packaged war using ProGuard")

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
      |-dontskipnonpubliclibraryclasses
      |-dontskipnonpubliclibraryclassmembers
      |-dontnote
      |-ignorewarnings
      |-keep public class bootstrap.liftweb.Boot {
      | public *;
      |}
      |-keep public class com.tngtech.** {
      | public *;
      |}
      |-keep public class * extends javax.servlet.** {
      | public *;
      |}
      |-keep public class javax.servlet.** {
      | public *;
      |}
      |-keep public class * extends org.apache.log4j.Appender {
      | public *;
      |}
      |-keep public class * extends org.apache.log4j.Layout {
      | public *;
      |}
      |"""

      val proguardConfiguration =
        outTemplate.stripMargin.format(
          mkpath(jarPath.asFile),
          mkpath(proguardJar.asFile))
			FileUtilities.write(proguardConfigurationPath.asFile, proguardConfiguration, log)
      None
  }

  private def proguardTask = task {
    FileUtilities.clean(proguardJar :: Nil, log)
    val proguardClasspath = managedClasspath(toolsConfig)
    val proguardClasspathString = Path.makeString(proguardClasspath.get)
    val configFile = proguardConfigurationPath.asFile.getAbsolutePath
    val exitValue = Process("java", List("-Xmx512M", "-cp", proguardClasspathString, "proguard.ProGuard", "@" + configFile)) ! log 
    if (exitValue == 0) None else Some("Proguard failed with nonzero exit code (" + exitValue + ")")
  }

	private def mkpath(f: File) = '\"' + f.getAbsolutePath + '\"'
}
