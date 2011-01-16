package com.tngtech.mmtaskspdfprinter.creation.jira

import scala.collection.JavaConversions._
import org.apache.xmlrpc._
import org.apache.xmlrpc.client._
import java.net.URL

import com.tngtech.mmtaskspdfprinter.scrum._

object JiraTaskCreator {
  val rpcPath = "/rpc/xmlrpc"
}

class JiraTaskCreator(val url: String, val user: String,
                      val pass: String, val project: String) {
  def create(backlogs: List[SprintBacklog]) {
    val rpcClient = new XmlRpcClient()
    val config = new XmlRpcClientConfigImpl()
    val normalizedUrl = """\/$""".r.replaceAllIn(url, "")
    config.setServerURL(new URL(normalizedUrl+JiraTaskCreator.rpcPath))
    rpcClient.setConfig(config)
    val loginToken = rpcClient.execute("jira1.login", List(user, pass)).toString
    val projects = rpcClient.execute("jira1.getProjectsNoSchemes", List(loginToken)).
                    asInstanceOf[Array[AnyRef]].
                      map(_.asInstanceOf[java.util.HashMap[String, String]])
    val projectKeys: List[String] = projects.map(_.get("key")).toList
    if (!projectKeys.exists(project == _)) {
      rpcClient.execute("jira1.logout", List(loginToken))
      throw new Exception("JIRA project "+project+" doesn't exist!\n" +
                          "Please create it or choose one of these projects: "+
                            projectKeys.mkString(", "))
    }

    for (b <- backlogs; s <- b.stories; t <- s.tasks) {
      val subtasks = t.subtasks.map("- "+_.description).mkString("\n")
      val category =
        if (t.category.isEmpty) ""
        else "Category: " + t.category + "\n"
      val args: java.util.Map[String, String] =
        Map(
          "project" -> project,
          "type" -> "3",
          "summary" -> (s.name+": "+t.description),
          "description" -> (
            "Sprint: " + s.name + "\n" +
            category +
            "Description: " + t.description + "\n" +
            subtasks )
        )
      rpcClient.execute("jira1.createIssue", List(loginToken, args))
    }
    rpcClient.execute("jira1.logout", List(loginToken))
  }
}
