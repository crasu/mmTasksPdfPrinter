package com.tngtech.mmtaskspdfprinter.model

import java.io.FileInputStream
import java.util.Properties
import java.io.BufferedInputStream
import java.io.File

class CentralConfiguration {
  protected val properties: Properties = {
    val file = new File("printer.props")
    val prop = new Properties
    if (file.exists) {
      prop.load(new BufferedInputStream(new FileInputStream(file)))
    }
    prop
  }
}