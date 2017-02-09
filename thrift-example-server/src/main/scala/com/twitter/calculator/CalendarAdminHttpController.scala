package com.twitter.calculator

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.twitter.calculator.db.{Day, DayService, Person}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.response.Mustache
import com.twitter.finatra.request.{FormParam, QueryParam}
import com.twitter.util.Await

@Mustache("person")
case class PersonView(person: Person)
@Mustache("insert")
case class DayView(
  days: List[Day]
)
@Mustache("insertresult")
case class DayInsertRequest(
  @FormParam insertCalendar: Int,
  @FormParam insertDate: String,
  @FormParam insertIsHoliday: Boolean
  //days: List[Day]
)
@Mustache("delete")
case class DeleteView(
  days: List[Day]
)
@Mustache("deleteresult")
case class DeleteRequestView(
  days: List[Day]
)

class CalendarAdminHttpController @Inject()(
  dayService: DayService
) extends Controller {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  get("/ping") { request: Request =>
    "pong"
  }

  get("/person") { request: Request =>
    PersonView(Person(1, "Alice"))
  }

  get("/delete") { request: Request =>
    DeleteView(Await.result(dayService.allDays))
  }

  get("/deleteresult") { request: Request =>
    Await.result(dayService.deleteAll)
    DeleteRequestView(Await.result(dayService.allDays))
  }

  get("/insert") { request: Request =>
    DayView(Await.result(dayService.allDays))
  }

  post("/insertresult") { request: DayInsertRequest =>
    val dayList = List(Day(request.insertCalendar, parseDate(request.insertDate), request.insertIsHoliday))
    Await.result(dayService.insertDays(dayList))
    request
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
  *      https://twitter.github.io/finatra/user-guide/http/controllers.html#controllers-and-routing
  * @see http://twitter.github.io/finatra/user-guide/files/
  */
  get("/:*") { request: Request =>
    response.ok.fileOrIndex(
      filePath = request.params("*"),
      indexPath = "index.html")
  }
}