package com.tngtech.mmtaskspdfprinter.creation.jira.config

import java.util.Properties
import com.tngtech.mmtaskspdfprinter.model.CentralConfiguration

trait JiraConfiguration extends CentralConfiguration {
  
  val hostname = properties.getProperty("jira.hostname", "")
  val project = properties.getProperty("jira.project", "")
  val user = properties.getProperty("jira.user", "")
  val password = properties.getProperty("jira.password", "")
  val issuetypename = properties.getProperty("jira.issuetypename","Task")
  val subissuetypename = properties.getProperty("jira.subissuetypename","Sub-task")
  
}
