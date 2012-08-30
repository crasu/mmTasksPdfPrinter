package com.tngtech.mmtaskspdfprinter.creation.jira
import org.apache.commons.httpclient._
import methods._
import multipart._
import java.io.ByteArrayOutputStream
import scala.io.Source
import scala.xml.PrettyPrinter

class SoapException(msg: String, e: Exception = null) extends Exception(msg, e)

trait SoapClient {
  import scala.xml.Elem
  def sendMessage(url: String, action: String, req: Elem): Elem
}

object TrivialSoapClient extends SoapClient {
      
  val htmlOk = 200
  
  import scala.xml.{ Elem, XML }

  def sendMessage(url: String, action: String, req: Elem) = {
    val postMethod = new PostMethod(url)
    try {
	    postMethod.setRequestEntity(new StringRequestEntity(wrap(req), "text/xml", "utf-8"))
	    postMethod.setRequestHeader("SOAPAction", action)    
	    val client = new HttpClient()
	    val status = client.executeMethod(postMethod)
	    val responseAsSource =
	      Source.fromInputStream(postMethod.getResponseBodyAsStream())
	    val responseAsString = responseAsSource.getLines.mkString
	    val response = XML.loadString(responseAsString)
	    val printer = new PrettyPrinter(1000, 2)
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

object JiraSoapMessages {
	private[jira] val jiraEndpoint = "/rpc/soap/jirasoapservice-v2"
	  
	private case class LoginSettings(val token: String, val project: String, 
	    val issueTypeId: String, val subissueTypeId: String)
}

class JiraSoapMessages(host: String, user: String, password: String, client: SoapClient = TrivialSoapClient) {
  
  private val url = host + JiraSoapMessages.jiraEndpoint
  
  import JiraSoapMessages.LoginSettings  
  private var loginSettings: Option[LoginSettings] = None
  
  def loginToProject(project: String, issueType: String, subissueType: String) = {
    val token = login()
    val projectId = getProjectByKey(token, project)
    val issuetypeId = getIssueTypeId(token, projectId, issueType)
    val subissuetypeId = getSubissueTypeId(token, projectId, subissueType)
    loginSettings = Some(LoginSettings(token, project, issuetypeId, subissuetypeId))
  }
  
  private def login(): String = {
    val req = 
          <soap:login soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{user}</in0>
    				<in1 xsi:type="xsd:string">{password}</in1>
    			</soap:login>
    val res = client.sendMessage(url, "login", req)
    val tokenRes = (res \\ "loginReturn").headOption
    tokenRes.get.text
  }
  
  def logout() {
    if (loginSettings.isDefined) {
	    val req = 
	          <soap:logout soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{loginSettings.get.token}</in0>
    			</soap:logout>  
	    client.sendMessage(url, "logout", req)    
	    loginSettings = None
    }
  }
  
  def createIssue(summary: String, description:String): String = {
    val req = 
        <soap:createIssue soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      		<in0 xsi:type="xsd:string">{loginSettings.get.token}</in0>
    			<in1>
    				<project>{loginSettings.get.project}</project>
    				<summary>{summary}</summary>
				<description>{description}</description>
    				<type>{loginSettings.get.issueTypeId}</type>
    			</in1>
    		</soap:createIssue>
    val res = client.sendMessage(url, "createIssue", req)
    (res \\ "key").head.text  	
  }
  
  def createSubissue(parentId: String, summary:String, description:String): String = {
    val req = 
        <soap:createIssueWithParent soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      			<in0 xsi:type="xsd:string">{loginSettings.get.token}</in0>
    				<in1>
    					<project>{loginSettings.get.project}</project>
    					<summary>{summary}</summary>
    					<description>{description}</description>
    					<type>{loginSettings.get.subissueTypeId}</type>
    				</in1>
    				<in2>{parentId}</in2>
    			</soap:createIssueWithParent>
    val res = client.sendMessage(url, "createIssueWithParent", req)
    (res \\ "key").head.text  	
  }  
  
  private def getProjectByKey(token: String, project: String) =  {
  	val req = 
      <soap:getProjectByKey soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token}</in0>
  			<in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getProjectByKey>
    val res = client.sendMessage(url, "getProjectByKey", req)
    println(new PrettyPrinter(1000,2).format(res))
    (res \\ "id").head.text  	
  }

  private def getIssueTypeId(token: String, project: String, issuetypeName: String) = {
    val req = 
      <soap:getIssueTypesForProject soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token}</in0>
    	  <in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getIssueTypesForProject>
    val res = client.sendMessage(url, "getIssueTypesForProject", req)
    val list = (res \\ "multiRef").map { el => 
      ((el \ "name").text, (el \ "id").text) 
    }
    val t = list.find{case (name, value) => name == issuetypeName}
    t.head._2
  } 
  
  private def getSubissueTypeId(token: String, project: String, issuetypeName: String) = {
    val req = 
      <soap:getSubTaskIssueTypesForProject soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
      	<in0 xsi:type="xsd:string">{token}</in0>
    	  <in1 xsi:type="xsd:string">{project}</in1>
    	</soap:getSubTaskIssueTypesForProject>
    val res = client.sendMessage(url, "getSubTaskIssueTypesForProject", req)
    val list = (res \\ "multiRef").map { el => 
      ((el \ "name").text, (el \ "id").text) 
    }
    val t = list.find{case (name, value) => name.toLowerCase == issuetypeName.toLowerCase}
    t.head._2
  }   
}
