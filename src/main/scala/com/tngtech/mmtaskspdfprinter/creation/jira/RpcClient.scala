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

  def castResponse(resp: Object): Array[java.util.HashMap[String, String]] =
    resp.asInstanceOf[Array[AnyRef]].
      map(_.asInstanceOf[java.util.HashMap[String, String]])

  def findProjectId(projectName: String) = {
    val projects = castResponse(rpcClient.execute("jira1.getProjectsNoSchemes", List(loginToken)))
    projects.find(projectName == _.get("key")) match {
      case None =>
        close()
        throw new Exception("JIRA project " + projectName + " doesn't exist!\n" +
          "Please create it or choose one of these projects: " +
          projects.map(_.get("key")).toList)
      case Some(project) => project.get("id")
    }
  }

  def findIssuetype(issuetypeName: String) = {
    val issuetypes = castResponse(rpcClient.execute("jira1.getIssueTypes", List(loginToken)))
    issuetypes.find(issuetypeName == _.get("name")) match {
      case None =>
        close()
        throw new Exception("No JIRA issue type named " + issuetypeName + ". Check the JIRA dashboard and set " +
          "jira.issuetypename to one of " + issuetypes.map(_.get("name")).toList)
      case Some(issuetype) => issuetype.get("id")
    }
  }

  def findSubissuetype(subissuetypeName: String) = {
    val subissuetypes = castResponse(rpcClient.execute("jira1.getSubTaskIssueTypes", List(loginToken)))
    subissuetypes.find(subissuetypeName == _.get("name")) match {
      case None =>
        close()
        throw new Exception("No JIRA subissue type named " + subissuetypeName + ". Check the JIRA dashboard and set " +
          "jira.subissuetypename to one of " + subissuetypes.map(_.get("name")).toList)
      case Some(subissuetype) => subissuetype.get("id")
    }
  }

  def createIssue(project: String, summary: String, issuetype: String) = {
    val args: java.util.Map[String, String] =
      Map(
        "project" -> project,
        "type" -> issuetype,
        "summary" -> summary,
        "description" -> summary)
    val issue = rpcClient.execute("jira1.createIssue", List(loginToken, args)).
      asInstanceOf[java.util.HashMap[String, Object]]
    RpcResponse(issue.get("id").toString, issue.get("key").toString)
  }

  def close() {
    rpcClient.execute("jira1.logout", List(loginToken))
  }
}

case class RpcResponse(id: String, key: String)