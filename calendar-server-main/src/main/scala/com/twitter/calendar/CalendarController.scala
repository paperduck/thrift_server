package com.twitter.calendar


import java.time.DayOfWeek
import java.util.Date

import com.twitter.calendar.thriftscala.Calendar
import com.twitter.calendar.thriftscala.Calendar._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.{Future}
import com.twitter.calendar.db._

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
class CalendarController @Inject()(dayService: DayService)
  extends Controller
  with thriftscala.Calendar.BaseServiceIface {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  // Since holidays are the inverse of business days, return the next non-holiday
  override val getNextBusinessDay = handle(GetNextBusinessDay) { args: GetNextBusinessDay.Args =>
    getNextBusinessDayRecursive(
      CalendarEnum.fromThriftCalendarToDb(args.calendar),
      Future.value(serializeDate(parseDate(args.startDate).plusDays(1))),
      100
    )
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
        val isHol = m.head || List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(d).getDayOfWeek)
        if (!isHol) {
          Future.value(d)
        } else {
          getNextBusinessDayRecursive(calendar, Future.value(serializeDate(parseDate(d).plusDays(1))), limit - 1)
        }
      })
    }
  }

  override val isTodayBusinessDay = handle(IsTodayBusinessDay) { args: IsTodayBusinessDay.Args =>
    //Date today = Calendar.getInstance().getTime()
    val today = LocalDate.now(ZoneOffset.UTC)
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(today.getDayOfWeek())
    val markedHoliday = dayService.isMarkedHoliday(CalendarEnum.fromThriftCalendarToDb(args.calendar), serializeDate(today))
    markedHoliday.map{mark =>
      if (mark.nonEmpty){
        if (mark.head) {
          // if it's marked as a holiday, then it's definitely not a business day
          false
        }else{
          // If it's not marked as a holiday, check if it's the weekend
          !isWeekend
        }
      }else{
        !isWeekend
      }
    }
  }

  override val isBusinessDay = handle(IsBusinessDay) { args: IsBusinessDay.Args =>
    val markedHoliday = dayService.isMarkedHoliday(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(args.date).getDayOfWeek())
    markedHoliday.map {mark =>
      if (mark.nonEmpty){
        if (mark.head) {
          // if it's marked as a holiday, then it's definitely not a business day
          false
        }else{
          // If it's not marked as a holiday, check if it's the weekend
          !isWeekend
        }
      }else{
        !isWeekend
      }
    }
  }

  // Return true if the day is marked as holiday in db OR is a weekend
  override val isHoliday = handle(IsHoliday) { args: IsHoliday.Args =>
    val queryResult = dayService.isMarkedHoliday(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.date)
    val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(parseDate(args.date).getDayOfWeek)
    queryResult.map {r =>
      isWeekend || (if (r.nonEmpty) r.head else false)
    }

  }

  override val insertDay = handle(InsertDay) { args: InsertDay.Args =>
    var success = true
    val newDay = Day(calendar=CalendarEnum.fromThriftCalendarToDb(args.calendar), date=parseDate(args.date),
      isHoliday=args.isHoliday)
    dayService.insertDays(List(newDay))
    Future.value(success)
  }

  override val getMarkedHolidays = handle(GetMarkedHolidays) { args: GetMarkedHolidays.Args =>
    val queryResult = dayService.getMarkedHolidays(CalendarEnum.fromThriftCalendarToDb(args.calendar), args.fromDate, args.toDate)
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
