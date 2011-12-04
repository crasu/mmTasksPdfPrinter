package com.tngtech.mmtaskspdfprinter.snippet;

import net.liftweb.http.{SessionVar, FileParamHolder, InMemoryResponse}
import net.liftweb.common.{Box, Full, Empty}
import com.tngtech.mmtaskspdfprinter.scrum.Sprint

object LastError extends SessionVar[Box[Exception]](Empty) {
  def showError() = if (this.isEmpty || is.isEmpty) {
    Empty
  } else {
    val msg = buildErrorMessage(is.get)
    Full(InMemoryResponse((<html><head></head><body><pre>{msg}</pre></body></html>).toString.getBytes,
                          List("Content-Type" -> "text/html"),
                          Nil,
                          200))
  }

  private def buildErrorMessage(error: Throwable): String = {
    val message = error.getMessage+"\n"+error.getStackTraceString+"\n"
    val rest = if (error.getCause == null) ""
    	else buildErrorMessage(error.getCause)
    message + rest
  }
}

class BacklogUpload extends Upload with Selection with Creation {
  protected object selectedBacklog extends SessionVar[Box[Sprint]](Empty)
  protected object uploadContainer extends SessionVar[Box[FileParamHolder]](Empty)
}
