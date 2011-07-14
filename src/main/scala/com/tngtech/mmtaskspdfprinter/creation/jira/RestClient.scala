package com.tngtech.mmtaskspdfprinter.creation.jira

import scala.collection.JavaConversions._
import java.io._
import org.apache.commons.httpclient._
import methods._
import multipart._

class JiraRestException(message: String) extends Exception(message)

class RestClient(val veryRawUrl: String, val user: String, val password: String) {
  private val rawUrl = """\/$""".r.replaceAllIn(veryRawUrl, "")

  val token = "os_username=" + user + "&os_password=" + password
  val url = rawUrl + "/secure/CreateSubTaskIssueDetails.jspa?" + token

  def createSubissue(projectId:String, parentId: String, summary:String, description:String,
      issuetype:String): String = {
    post(
      Map(),
      Map(
        "summary" -> summary,
        "priority" -> "3",
        "duedate" -> "",
        "assignee" -> "-1",
        "reporter" -> user,
        "environment" -> "",
        "description" -> description,
        "timetracking_originalestimate" -> "",
        "timetracking_remainingestimate" -> "",
        "isCreateIssue" -> "true",
        "hasWorkStarted" -> "",
        "issuetype" -> issuetype,
        "viewIssueKey" -> "",
        "pid" -> projectId,
        "parentIssueId" -> parentId,
        "Create" -> "Create"))
  }

  def post(params: Map[String, String], parts: Map[String, String]): String = {
    val stringParams = params map { case (k, v) => new NameValuePair(k, v) }
    val stringParts = parts map {
      case (k, v) =>
        val part = new StringPart(k, v)
        /* Unfortunatly JIRA doesn't parse transfer encodings correctly */
        part.setTransferEncoding(null)
        part.setContentType(null)
        part
    }
    val postMethod = new PostMethod(url)
    postMethod.addParameters(stringParams.toArray)
    val bout = new ByteArrayOutputStream();
    val request = new MultipartRequestEntity(stringParts.toArray, postMethod.getParams)
    postMethod.setRequestEntity(request)
    request.writeRequest(bout)
    val client = new HttpClient()
    val status = client.executeMethod(postMethod)
    val response = postMethod.getResponseBodyAsString()
    val locationHeader = postMethod.getResponseHeader("Location")
    if (response.contains("form-message error") || (status != 200 && status != 302)) {
      throw new JiraRestException(response)
    }
    /* JIRA redirects to new issue after posting, so can extract ID from Location-URL */
    locationHeader.toString().split("/").last
  }
}
