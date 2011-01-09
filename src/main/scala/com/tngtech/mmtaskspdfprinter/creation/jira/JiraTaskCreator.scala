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

    for (b <- backlogs; s <- b.stories; t <- s.tasks) {
      val subtasks = t.subtasks.map("- "+_).mkString("\n")
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
  }
}
