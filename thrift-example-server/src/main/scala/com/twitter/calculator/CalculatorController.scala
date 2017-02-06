package com.twitter.calculator


import com.twitter.calculator.thriftscala.Calendar
import com.twitter.calculator.thriftscala.Calendar._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import com.twitter.calculator.db._

//import com.twitter.calculator.db.PersonService

//import scala.concurrent.duration._

import javax.inject.{Inject, Singleton}
import java.time.LocalDate
import java.time.format.DateTimeFormatter


sealed trait CalendarEnum {
  def value: String
  def int: Int
}

object CalendarEnum {
  case object JPX       extends CalendarEnum {val value = "JPX";        val int = 0}
  case object Japannext extends CalendarEnum {val value = "Japannext";  val int = 1}
  case object NASDAQ    extends CalendarEnum {val value = "NASDAQ";     val int = 2}

  def fromString(value: String): CalendarEnum = value match{
    case CalendarEnum.JPX.value       => CalendarEnum.JPX
    case CalendarEnum.Japannext.value => CalendarEnum.Japannext
    case CalendarEnum.NASDAQ.value    => CalendarEnum.NASDAQ
  }

  def fromInt(value: Int): CalendarEnum = value match{
    case CalendarEnum.JPX.int       => CalendarEnum.JPX
    case CalendarEnum.Japannext.int => CalendarEnum.Japannext
    case CalendarEnum.NASDAQ.int    => CalendarEnum.NASDAQ
  }

  // convert from thrift's enum to Scala's enum
  def fromThriftCalendarToDb(value: thriftscala.CalendarEnum): Int = value match{
    case thriftscala.CalendarEnum.Jpx       => CalendarEnum.JPX.int
    case thriftscala.CalendarEnum.Japannext => CalendarEnum.Japannext.int
    case thriftscala.CalendarEnum.Nasdaq    => CalendarEnum.NASDAQ.int
  }
}

@Singleton
//class CalculatorController @Inject()(personService: PersonService)//PersonService is here just to demonstrate how to use service in controller
class CalculatorController @Inject()(dayService: DayService)
  extends Controller
  with Calendar.BaseServiceIface {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  override val isHoliday = handle(IsHoliday) { args: IsHoliday.Args =>
    val queryResult = dayService.isHoliday(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
    // not invoked until the integer value becomes available
    val result = queryResult.map {r => {
      if (r.length > 0) {
        r.head
      }
      else
      {
        false
      }
    }}
    result
  }

  override val insertDay = handle(InsertDay) { args: InsertDay.Args =>
    var success = true
    info(s"Adding day...")
    val newDay = Day(calendar=CalendarEnum.fromThriftCalendarToDb(args.calendar), date=parseDate(args.date),
      isHoliday=args.isHoliday)
    val queryResult = dayService.insertDays(List(newDay))
    Future.value(success)
    //queryResult.map({x => info("Finished"); x})
  }

  override val getHolidays = handle(GetHolidays) { args: GetHolidays.Args =>
    info(s"Getting holidays...")
    val queryResult = dayService.getHolidays(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.fromDate, args.toDate)
    //queryResult.map {r => r(0)}  // extract from Future
    // date comes from db as date; convert to string
    queryResult.map{row => row.map(elem => serializeDate(elem))}
  }

  override val countDays = handle(CountDays) { args: CountDays.Args =>
    dayService.countDays
  }

  override val deleteAll = handle(DeleteAll) { args: DeleteAll.Args =>
    dayService.deleteAll
  }

  /**************************************************************************************/

  override val addNumbers = handle(AddNumbers) { args: AddNumbers.Args =>
    info(s"Adding numbers $args.a + $args.b")
    Future.value(args.a + args.b)
  }

  override val addStrings = handle(AddStrings) { args: AddStrings.Args =>
    Future.value(
      (args.a.toInt + args.b.toInt).toString)
  }

  override val increment = handle(Increment) { args: Increment.Args =>
    Future.value(args.a + 1)
  }
}
