package com.tngtech.mmtaskspdfprinter.creation.jira.config

import java.util.Properties

trait JiraConfiguration {
  self: {def properties: Properties} =>
  
  val hostname = properties.getProperty("jira.hostname", "")
  val project = properties.getProperty("jira.project", "")
}
