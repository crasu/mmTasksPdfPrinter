package com.tngtech.mmtaskspdfprinter.snippet;

import net.liftweb.http.{SessionVar, FileParamHolder, InMemoryResponse}
import net.liftweb.common.{Box, Full, Empty}
import com.tngtech.mmtaskspdfprinter.scrum.Sprint

object LastError extends SessionVar[Box[Exception]](Empty) {
  def showError() = if (this.isEmpty || is.isEmpty) {
    Empty
  } else {
      is.get.getCause.printStackTrace()
    Full(InMemoryResponse(is.get.getCause.getMessage.getBytes,
                          List("Content-Type" -> "text/html"),
                          Nil,
                          200))
  }

  private def buildErrorMessage(error: Throwable): String =
    if (error.getCause == null) {
      error.getMessage
    } else {
      error.getMessage + "\n" + buildErrorMessage(error.getCause)
    }
}

class BacklogUpload extends Upload with Selection with Creation {
  protected object selectedBacklog extends SessionVar[Box[Sprint]](Empty)
  protected object uploadContainer extends SessionVar[Box[FileParamHolder]](Empty)
}
