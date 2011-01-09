package com.tngtech.mmtaskspdfprinter.snippet;

import net.liftweb.http.{SessionVar, FileParamHolder}
import net.liftweb.common.{Box, Full, Empty}
import com.tngtech.mmtaskspdfprinter.scrum.SprintBacklog

class BacklogUpload extends Upload with Selection with Creation {
  protected object selectedBacklog extends SessionVar[Box[SprintBacklog]](Empty)
  protected object uploadContainer extends SessionVar[Box[FileParamHolder]](Empty)
}
