package com.twitter.calculator

import java.time.{DayOfWeek, LocalDate}
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.twitter.calculator.db.{Day, DayService, Person}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.response.Mustache
import com.twitter.finatra.request.{FormParam, QueryParam}
import com.twitter.util.{Await, Future}


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
@Mustache("deletewhere")
case class DeleteWhereView(
  days: List[Day]
)
@Mustache("deletewhereresult")
case class DeleteWhereRequest(
  @FormParam calendar: Int,
  @FormParam date: String
  //days: List [Day]
)
@Mustache("isholiday")
case class IsHolidayView(
  days: List[Day]
)
@Mustache("isholidayresult")
case class IsHolidayRequest(
  @FormParam calendar: Int,
  @FormParam date: String
)
@Mustache("isholidayresult")
case class IsHolidayResult(
  isHolResult: Boolean,
  days: List[Day]
)
// @Mustache("isbusinessday")
@Mustache("getnextbusinessday")
case class GetNextBusinessDayView(
  days: List[Day]
)
@Mustache("getnextbusinessdayresult")
case class GetNextBusinessDayRequest(
  @FormParam calendar: Int,
  @FormParam startDate: String
)
@Mustache("getnextbusinessdayresult")
case class GetNextBusinessDayResponse(
  resultDay: String,
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

  get("/insert") { request: Request =>
    DayView(Await.result(dayService.allDays))
  }

  post("/insertresult") { request: DayInsertRequest =>
    val dayList = List(Day(request.insertCalendar, parseDate(request.insertDate), request.insertIsHoliday))
    Await.result(dayService.insertDays(dayList))
    request
  }

  get("/delete") { request: Request =>
    DeleteView(Await.result(dayService.allDays))
  }

  get("/deleteresult") { request: Request =>
    Await.result(dayService.deleteAll)
    DeleteRequestView(Await.result(dayService.allDays))
  }

  get("/deletewhere") { request: Request =>
    DeleteWhereView(Await.result(dayService.allDays))
  }

  post("/deletewhereresult") { request: DeleteWhereRequest =>
    Await.result(dayService.deleteOne(request.calendar, request.date))
    request
  }

  get("/isholiday") { request: Request =>
    IsHolidayView(Await.result(dayService.allDays))
  }

  get("/isholidayresult") { request: IsHolidayRequest =>
    var markedAsHoliday = Await.result(dayService.isHoliday(request.calendar, request.date))
    if (markedAsHoliday.isEmpty) markedAsHoliday = List(false) // default value
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(request.date).getDayOfWeek)
    val isHol = markedAsHoliday.head || isWeekend
    val dayList = Await.result(dayService.allDays)
    IsHolidayResult(isHolResult = isHol, days = dayList)

    /*
    for {
      res <- dayService.isHoliday(request.calendar, request.date )  // <- is like flat map
      dayList <- dayService.allDays
    } yield {
      IsHolidayResult(res(0), dayList)
    }

    // transform Seq[Future[_]] to Future[Seq[_]]
    // The futures have to all be the same type.
    val x = Future.collect(Seq(
      dayService.isHoliday(request.calendar, request.date),
      dayService.allDays
    ))
    x.map(x=> IsHolidayResult(x(0)(0), x(1)))
    */
  }

  get("/getnextbusinessday") { request: Request =>
    GetNextBusinessDayView(Await.result(dayService.allDays))
  }

  /* copied from CalculatorController */
  get("/getnextbusinessdayresult") { request: GetNextBusinessDayRequest =>
      val resultDay = getNextBusinessDayRecursive(request.calendar, serializeDate(parseDate(request.startDate).plusDays(1)), 100)
      val dayList = Await.result(dayService.allDays)
      GetNextBusinessDayResponse(resultDay, dayList)
  }

  /* copied from CalculatorController */
  def getNextBusinessDayRecursive (calendar: Int, dateKey: String, limit: Int): String = {
    if (limit == 0) throw new Exception // reached limit
    // dayService.isHoliday might return empty list
    var markedAsHoliday = Await.result(dayService.isHoliday(calendar, dateKey))
    if (markedAsHoliday.isEmpty) {
      markedAsHoliday = List(false)
    }
    // dayService doesn't take weekend into consideration
    val isHolOrWeekend = markedAsHoliday.head || List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(dateKey).getDayOfWeek())
    if (!isHolOrWeekend){
      dateKey
    }else{
      getNextBusinessDayRecursive(calendar, serializeDate(parseDate(dateKey).plusDays(1)), limit - 1)
    }
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