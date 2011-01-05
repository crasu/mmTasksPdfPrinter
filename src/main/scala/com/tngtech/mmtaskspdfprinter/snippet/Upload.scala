package com.tngtech.mmtaskspdfprinter.snippet

import scala.xml.{NodeSeq, Group}
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.common.{Box, Full, Empty}

/**
 *Step 1: File Uplaod
*/
trait Upload {
  self: BacklogUpload =>

  def upload(xhtml: Group): NodeSeq =
    bind("upload", chooseTemplate("choose", "upload", xhtml),
      "fileUpload" -> SHtml.fileUpload {ul =>
        uploadContainer(Full(ul))
        selectedBacklog(Empty)
    })
}
