package com.tngtech.mmtaskspdfprinter.creation.jira

import scala.collection.JavaConversions._
import java.io._
import org.apache.commons.httpclient._
import methods._ 
import multipart._

class JiraRestException (message: String) extends Exception(message)

class JiraRestClient(val url: String) {

  def post(params: Map[String, String],
           parts: Map[String, String]) {
    val stringParams = params map {case (k, v) => new NameValuePair(k, v)}
    val stringParts = parts map {case (k, v) => 
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
    if (response.contains("form-message error") || (status != 200 && status != 302)) {
      throw new JiraRestException("Jira Responeded with an error. Status: "+status+"------------------------\n"+bout+"------------------------\n"+response)
    }
  }
}