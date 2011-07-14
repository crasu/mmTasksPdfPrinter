package com.tngtech.mmtaskspdfprinter.creation.jira

import org.apache.xmlrpc._
import org.apache.xmlrpc.client._
import java.net.URL
import scala.collection.JavaConversions._

class RpcClient(rawUrl: String, val user: String, val pass: String) {

  private val url = """\/$""".r.replaceAllIn(rawUrl, "")

  private val rpcClient = try {
    val rpcClient = new XmlRpcClient()
    val config = new XmlRpcClientConfigImpl()
    config.setServerURL(new URL(url + JiraTaskCreator.rpcPath))
    rpcClient.setConfig(config)
    rpcClient
  } catch {
    case ex: org.apache.xmlrpc.XmlRpcException => throw new JiraException("Failed to setup connection to JIRA", ex)
  }

  private val loginToken = try {
    rpcClient.execute("jira1.login", List(user, pass)).toString
  } catch {
    case ex: org.apache.xmlrpc.XmlRpcException => throw new JiraException("Failed to login to JIRA", ex)
  }

  def findProjectId(projectName: String) = {
    val projects = rpcClient.execute("jira1.getProjectsNoSchemes", List(loginToken)).
      asInstanceOf[Array[AnyRef]].
      map(_.asInstanceOf[java.util.HashMap[String, String]])
    projects.find(projectName == _.get("key")) match {
      case None =>
        close()
        throw new Exception("JIRA project " + projectName + " doesn't exist!\n" +
          "Please create it or choose one of these projects: " +
          projects.map(_.get("key")).toList)
      case (project: java.util.HashMap[String, String]) => project.get("id")
    }
  }

  case class RpcResponse(id: String, key: String)

  def createIssue(project: String, summary: String) = {
    val args: java.util.Map[String, String] =
      Map(
        "project" -> project,
        "type" -> "3",
        "summary" -> summary,
        "description" -> summary)
    val issue = rpcClient.execute("jira1.createIssue", List(loginToken, args)).asInstanceOf[java.util.HashMap[String, Object]]
    RpcResponse(issue.get("id").toString, issue.get("key").toString)
  }

  def close() {
    rpcClient.execute("jira1.logout", List(loginToken))
  }
}