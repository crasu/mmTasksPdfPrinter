package com.tngtech.mmtaskspdfprinter.creation.jira
import org.apache.commons.httpclient._
import methods._
import multipart._
import java.io.ByteArrayOutputStream
import scala.io.Source
import scala.xml.PrettyPrinter

class SoapException(msg: String, e: Exception = null) extends Exception(msg, e)

object SoapClient {
    
  val jiraEndpoint = "/rpc/soap/jirasoapservice-v2"
    
  val htmlOk = 200
  
  import scala.xml.{ Elem, XML }

  private def sendMessage(host: String, action: String, req: Elem) = 
  	post(host+jiraEndpoint, action, req)
  
  private def post(url: String, action: String, req: Elem): Elem = {
    val postMethod = new PostMethod(url)
    try {
	    postMethod.setRequestEntity(new StringRequestEntity(wrap(req), "text/xml", "utf-8"))
	    postMethod.setRequestHeader("SOAPAction", action)    
	    val client = new HttpClient()
	    client.getParams().setSoTimeout(10000)
	    client.setConnectionTimeout(10000)
	    val status = client.executeMethod(postMethod)
	    val responseAsSource =
	      Source.fromInputStream(postMethod.getResponseBodyAsStream())
	    val responseAsString = responseAsSource.getLines.mkString
	    val response = XML.loadString(responseAsString)
	    if (status != htmlOk) {
	    	handleError(req, response)
	    }
      response
    } finally {
      if (postMethod != null) postMethod.releaseConnection()
    }
  }
  
  private def handleError(req: Elem, res: Elem) = {
	  val printer = new PrettyPrinter(1000, 2)
	  val delimiter = "-" * 200
	  val error = (res \\ "faultstring").text 
	  throw new SoapException("JIRA error: " + error + "\n" +
	      "Send:\n" + 
	      printer.format(req) + "\n" +
	      delimiter + "\n" +
	      "Received:\n" +
	      printer.format(res)
	  )
  }
  
  private def wrap(xml: Elem): String = {
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
  		<soapenv:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
  			xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  			xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:soap="http://soap.rpc.jira.atlassian.com">
  		<soapenv:Header/>
  			<soapenv:Body>
  			{xml}
  			</soapenv:Body>
  		</soapenv:Envelope>.toString
  }  
}

class SoapClient(host: String, user: String, password: String) {
  
  private var token: Option[String] = None
  
  def login(): String = {
    val req = 
          <soap:login soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{user}</in0>
    				<in1 xsi:type="xsd:string">{password}</in1>
    			</soap:login>
    val res = SoapClient.sendMessage(host, "login", req)
    val tokenRes = (res \\ "loginReturn").headOption
    token = Some(tokenRes.get.text)
    token.get
  }
  
  def logout() {
    if (token.isDefined) {
	    val req = 
	          <soap:logout soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{token.get}</in0>
    			</soap:logout>  
	    SoapClient.sendMessage(host, "logout", req)    
	    token = None
    }
  }
  
  def createIssue(project: String, summary: String, issuetype: String): String = {
    val projectId = getProjectByKey(project)
    val typeId = getIssueTypeId(projectId, issuetype)
    val req = 
        <soap:createIssue soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{token.get}</in0>
    				<in1>
    					<project>{project}</project>
    					<summary>{summary}</summary>
    					<type>{typeId}</type>
    				</in1>
    			</soap:createIssue>
    val res = SoapClient.sendMessage(host, "createIssue", req)
    (res \\ "key").head.text  	
  }
  
  def createSubissue(project:String, parentId: String, summary:String, description:String,
      issuetype:String): String = {
    val projectId = getProjectByKey(project)
    val typeId = getSubIssueTypeId(projectId, issuetype)
    val req = 
        <soap:createIssueWithParent soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{token.get}</in0>
    				<in1>
    					<project>{project}</project>
    					<summary>{summary}</summary>
    					<description>{description}</description>
    					<type>{typeId}</type>
    				</in1>
    				<in2>{parentId}</in2>
    			</soap:createIssueWithParent>
    val res = SoapClient.sendMessage(host, "createIssueWithParent", req)
    (res \\ "key").head.text  	
  }  
  
  private def getProjectByKey(project: String) =  {
  	val req = 
      <soap:getProjectByKey soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token.get}</in0>
  			<in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getProjectByKey>
    val res = SoapClient.sendMessage(host, "getProjectByKey", req)
    println(new PrettyPrinter(1000,2).format(res))
    (res \\ "id").head.text  	
  }

  private def getIssueTypeId(project: String, issuetypeName: String) = {
    val req = 
      <soap:getIssueTypesForProject soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token.get}</in0>
    	  <in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getIssueTypesForProject>
    val res = SoapClient.sendMessage(host, "getIssueTypesForProject", req)
    val list = (res \\ "multiRef").map { el => 
      ((el \ "name").text, (el \ "id").text) 
    }
    val t = list.find{case (name, value) => name == issuetypeName}
    t.head._2
  } 
  
  private def getSubIssueTypeId(project: String, issuetypeName: String) = {
    val req = 
      <soap:getSubTaskIssueTypesForProject soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token.get}</in0>
    	  <in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getSubTaskIssueTypesForProject>
    val res = SoapClient.sendMessage(host, "getSubTaskIssueTypesForProject", req)
    val list = (res \\ "multiRef").map { el => 
      ((el \ "name").text, (el \ "id").text) 
    }
    val t = list.find{case (name, value) => name.toLowerCase == issuetypeName.toLowerCase}
    t.head._2
  }   
}