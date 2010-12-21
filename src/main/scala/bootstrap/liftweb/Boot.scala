package bootstrap.liftweb

import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import _root_.com.tngtech.mmtaskspdfprinter.snippet._
import Helpers._

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("com.tngtech.mmtaskspdfprinter")

    // Build SiteMap
    val entries = Menu(Loc("Home", List("index"), "Home")) ::
                  Menu(Loc("Upload", List("backlog_upload"), "Upload")) ::
                  Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))

    // Add statefull dispatchers
    LiftRules.dispatch.append {
      case Req(List("your_tasks"), _, _)  =>
        () => TaskCreator.reply()
    }

  }
}

