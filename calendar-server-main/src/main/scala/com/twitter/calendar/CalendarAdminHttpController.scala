package com.twitter.calendar

import java.time.{DayOfWeek, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import com.twitter.calendar.db.{Day, DayService, Person}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.response.Mustache
import com.twitter.finatra.request.{FormParam, QueryParam}
import com.twitter.util.Future


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
)
@Mustache("insertresult")
case class InsertResponse(
  days: List[Day],
  errorMsg: String
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
@Mustache("deletewhereresult")
case class DeleteWhereResponse(
  days: List[Day],
  delResult: Long
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
@Mustache("istodaybusinessday")
case class IsTodayBusinessDayView(
  days: List[Day]
)
@Mustache("istodaybusinessdayresult")
case class IsTodayBusinessDayRequest(
   @FormParam calendar: Int
 )
@Mustache("istodaybusinessdayresult")
case class IsTodayBusinessDayResponse(
  result: Boolean,
  days: List[Day]
)
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

  get("/insert") { request: Request =>
    val dayList = dayService.allDays
    dayList.map{x => response.ok.body(DayView(x))}
  }

  post("/insertresult") { request: DayInsertRequest =>
    val newDays = List(Day(request.insertCalendar, parseDate(request.insertDate), request.insertIsHoliday))
    var errorMsg = ""
      // Use flatMap to ensure that the insert happens before retrieving the list of days.
        dayService.insertDays(newDays).rescue{
          case dbError: com.twitter.finagle.mysql.ServerError =>
            println(s"ERROR: Does that day already exist in the database?")
            errorMsg = "Database error while inserting. Does the day already exist in the database?"
            dayService.allDays.map { dayList => InsertResponse(dayList, errorMsg) }
          case _: Throwable =>
            println("ERROR while inserting.")
            dayService.allDays.map { dayList => InsertResponse(dayList, errorMsg) }
        }.flatMap { x =>
          dayService.allDays.map { dayList => InsertResponse(dayList, errorMsg) }
        }
  }

  get("/delete") { request: Request =>
    val dayList = dayService.allDays
    dayList.map{x => response.ok.body(DeleteView(x))}
  }

  get("/deleteresult") { request: Request =>
    // Use flatMap to ensure the delete happens before getting the list.
    Future().flatMap { x =>
      dayService.deleteAll
      dayService.allDays.map { x => DeleteRequestView(x) }
    }
  }

  get("/deletewhere") { request: Request =>
    dayService.allDays.map{x => DeleteWhereView(x)}
  }

  post("/deletewhereresult") { request: DeleteWhereRequest =>
    // Use flatMap to ensure the delete happens before getting the list.
      dayService.deleteOne(request.calendar, request.date).flatMap { delResult =>
        dayService.allDays.map { x => DeleteWhereResponse(x, delResult) }
      }
  }

  get("/isholiday") { request: Request =>
    dayService.allDays.map{x => IsHolidayView(x)}
  }

  get("/isholidayresult") { request: IsHolidayRequest =>
    val markedAsHoliday = dayService.isMarkedHoliday(request.calendar, request.date)
    val isMarked = markedAsHoliday.map{x:List[Boolean] => if (x.isEmpty) List(false) else x} // default value
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(request.date).getDayOfWeek)
    isMarked.flatMap{m =>
      dayService.allDays.map{dayList => IsHolidayResult(isHolResult = m.head || isWeekend, days = dayList)}
    }
  }

  get("/istodaybusinessday") { request: Request =>
    dayService.allDays.map{x => IsTodayBusinessDayView(x)}
  }

  get("/istodaybusinessdayresult") { request: IsTodayBusinessDayRequest =>
    val today = serializeDate(LocalDate.now(ZoneOffset.UTC))
    val markedAsHoliday = {
      dayService.isMarkedHoliday(request.calendar, today)
        .map{x:List[Boolean] =>
          // If the holiday is not in the database, then it's "not marked as holiday" by default.
          if (x.isEmpty) List(false) else x
        }
    }
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(today).getDayOfWeek)
    markedAsHoliday.flatMap{m =>
      dayService.allDays.map{dayList => IsTodayBusinessDayResponse(result = !m.head && !isWeekend, days = dayList)}
    }
  }

  get("/getnextbusinessday") { request: Request =>
    dayService.allDays.map{x => GetNextBusinessDayView(x)}
  }

  get("/getnextbusinessdayresult") { request: GetNextBusinessDayRequest =>
      val resultDay:Future[String] = getNextBusinessDayRecursive(
        request.calendar,
        Future.value(serializeDate(parseDate(request.startDate).plusDays(1))),
        100
      )
      dayService.allDays.flatMap{ dayList =>
        resultDay.map{ d =>
          GetNextBusinessDayResponse(d, dayList)
      }}
  }

  def getNextBusinessDayRecursive (calendar: Int, dateKey: Future[String], limit: Int):Future[String] = {
    if (limit == 0) throw new Exception // reached limit
    // dayService.isMarkedHoliday might return empty list
    dateKey.flatMap { d =>
      val markedAsHoliday = dayService.isMarkedHoliday(calendar, d).map { x =>
        if (x.isEmpty) List(false) else x
      }
      // dayService doesn't take weekend into consideration, so do it here.
      markedAsHoliday.flatMap({ m =>
        // If the date is marked as Holiday OR the weekend, then it's not a business day.
        val isHol = m.head || List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(d).getDayOfWeek)
        if (!isHol) {
          Future.value(d)
        } else {
          getNextBusinessDayRecursive(calendar, Future.value(serializeDate(parseDate(d).plusDays(1))), limit - 1)
        }
      })
    }
  }

  get("/:*") { request: Request =>
    response.ok.fileOrIndex(
      filePath = request.params("*"),
      indexPath = "index.html")
  }
}