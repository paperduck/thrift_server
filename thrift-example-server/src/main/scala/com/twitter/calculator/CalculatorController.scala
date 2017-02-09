package com.twitter.calculator


import java.time.DayOfWeek
import java.util.Date

import com.twitter.calculator.thriftscala.Calendar
import com.twitter.calculator.thriftscala.Calendar._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.{Await, Future}
import com.twitter.calculator.db._

//import com.twitter.calculator.db.PersonService

import javax.inject.{Inject, Singleton}
import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Calendar


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
  with thriftscala.Calendar.BaseServiceIface {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  // Since holidays are the inverse of business days, return the next non-holiday
  override val getNextBusinessDay = handle(GetNextBusinessDay) { args: GetNextBusinessDay.Args =>
    Future.value(getNextBusinessDayRecursive(args.calendar, args.startDate, 100))
  }

  def getNextBusinessDayRecursive (calendar: thriftscala.CalendarEnum, dateKey: String, limit: Int): String = {
    if (limit == 0) throw new Exception // reached limit
    var isHol = Await.result(dayService.isHoliday(CalendarEnum.fromThriftCalendarToDb(calendar), dateKey))
    if (isHol.isEmpty) {
      info("isHol.isEmpty == true")
      throw new Exception
    }
    info(s"isHol(0) before weekend processing: ${isHol(0)}")
    // dayService doesn't take weekend into consideration
    val isHolOrWeekend = isHol(0) || List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(dateKey).getDayOfWeek())
    info(s"isHol(0) after weekend processing: ${isHol(0)}")
    if (!isHolOrWeekend){
      dateKey
    }else{
      getNextBusinessDayRecursive(calendar, serializeDate(parseDate(dateKey).plusDays(1)), limit - 1)
    }
  }

  override val isTodayBusinessDay = handle(IsTodayBusinessDay) { args: IsTodayBusinessDay.Args =>
    //Date today = Calendar.getInstance().getTime()
    val today = LocalDate.now(ZoneOffset.UTC)
    val queryResult = dayService.isBusinessDay(CalendarEnum.fromThriftCalendarToDb(args.calendar), serializeDate(today))
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(today.getDayOfWeek())
    val result = queryResult.map {r => {
      !isWeekend && (if (r.nonEmpty) r.head else false) // empty set should throw exception
    }}
    result
  }

  override val isBusinessDay = handle(IsBusinessDay) { args: IsBusinessDay.Args =>
    val queryResult = dayService.isBusinessDay(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(args.date).getDayOfWeek())
    val result = queryResult.map {r => {
      !isWeekend && (if (r.nonEmpty) r.head else true)
    }}
    result
  }

  // Return true if the day is marked as holiday in db OR is a weekend
  override val isHoliday = handle(IsHoliday) { args: IsHoliday.Args =>
    val queryResult = dayService.isHoliday(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(args.date).getDayOfWeek())
    val result = queryResult.map {r => {
      isWeekend || (if (r.nonEmpty) r.head else false)
    }}
    result
  }

  override val insertDay = handle(InsertDay) { args: InsertDay.Args =>
    var success = true
    val newDay = Day(calendar=CalendarEnum.fromThriftCalendarToDb(args.calendar), date=parseDate(args.date),
      isHoliday=args.isHoliday)
    val queryResult = dayService.insertDays(List(newDay))
    Future.value(success)
    //queryResult.map({x => info("Finished"); x})
  }

  override val getHolidays = handle(GetHolidays) { args: GetHolidays.Args =>
    val queryResult = dayService.getHolidays(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.fromDate, args.toDate)
    //queryResult.map {r => r(0)}  // extract from Future
    // date comes from db as date; convert to string
    queryResult.map{row => row.map(elem => serializeDate(elem))}
  }

  override val countDays = handle(CountDays) { args: CountDays.Args =>
    dayService.countDays
  }

  override val deleteOne = handle(DeleteOne) { args: DeleteOne.Args =>
    dayService.deleteOne(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
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
