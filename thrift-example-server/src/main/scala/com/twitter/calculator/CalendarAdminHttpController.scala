package com.twitter.calculator

import javax.inject.Inject

import com.twitter.calculator.db.DayService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class CalendarAdminHttpController @Inject()(
  dayService: DayService
) extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }

  /**
  * An example of how to serve files or an index. If the path param of "*" matches the name/path
  * of a file that can be resolved by the [[com.twitter.finatra.http.routing.FileResolver]]
  * then the file will be returned. Otherwise the file at 'indexPath' (in this case 'index.html')
  * will be returned. This is useful for building "single-page" web applications.
  *
  * Routes a are matched in the order they are defined, thus this route SHOULD be LAST as it is
  * a "catch-all" and routes should be defined in order of most-specific to least-specific.
    *
  * @see http://twitter.github.io/finatra/user-guide/build-new-http-server/controller.html#controllers-and-routing
  * @see http://twitter.github.io/finatra/user-guide/files/
  */
  get("/:*") { request: Request =>
    response.ok.fileOrIndex(
      filePath = request.params("*"),
      indexPath = "index.html")
  }
}