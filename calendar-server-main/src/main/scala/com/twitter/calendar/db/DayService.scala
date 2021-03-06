package com.twitter.calendar.db

import java.time.LocalDate
import java.util.Date
import javax.inject.{Inject, Singleton}
import java.time.format.DateTimeFormatter

import com.twitter.calendar.{CalendarEnum, thriftscala}
import io.getquill.context.Context
import io.getquill.context.sql.SqlContext
import io.getquill._

/*
//encoding logic which can be mixed in with different database contexts
trait LocalDateEncoder { this: Context[_, _] =>
  implicit val encodeLocalDate = MappedEncoding[LocalDate, String](date => date.toString())
  implicit val decodeLocalDate = MappedEncoding[String, LocalDate](str => LocalDate.parse(str))

  implicit class ForLocalDate(ldt: LocalDate) {
    def > = quote((arg: LocalDate) => infix"$ldt > $arg".as[Boolean])
    def >= = quote((arg: LocalDate) => infix"$ldt >= $arg".as[Boolean])
    def < = quote((arg: LocalDate) => infix"$ldt < $arg".as[Boolean])
    def <= = quote((arg: LocalDate) => infix"$ldt <= $arg".as[Boolean])
    def == = quote((arg: LocalDate) => infix"$ldt = $arg".as[Boolean])
  }
  def now = quote(infix"now()".as[LocalDate])
}
*/

@Singleton
class DayService @Inject()(val ctx: FinagleMysqlContext[Literal]){
  import ctx._
  implicit val encodeLocalDate = MappedEncoding[LocalDate, String](_.toString())
  implicit val decodeLocalDate = MappedEncoding[String, LocalDate](LocalDate.parse(_))

  implicit class ForLocalDate(ldt: LocalDate) {
    def > = quote((arg: LocalDate) => infix"$ldt > $arg".as[Boolean])
    def >= = quote((arg: LocalDate) => infix"$ldt >= $arg".as[Boolean])
    def < = quote((arg: LocalDate) => infix"$ldt < $arg".as[Boolean])
    def <= = quote((arg: LocalDate) => infix"$ldt <= $arg".as[Boolean])
    def == = quote((arg: LocalDate) => infix"$ldt = $arg".as[Boolean])
  }

  /* This raw SQL infix query gets a compile-time error.
   * Submitted an issue on GitHub:
   * https://github.com/getquill/quill/issues/708
   */
  //def rowCount = quote(infix"""SELECT ROW_COUNT()""".as[Query[Long]])
  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)
  implicit val encodeCalendarEnum = MappedEncoding[CalendarEnum, Int](_.int)
  implicit val decodeCalendarEnum = MappedEncoding[Int, CalendarEnum](CalendarEnum.fromInt)

  /*
  // Doesn't take weekends into consideration.
  def isBusinessDay(calendar: Int, date: String) = ctx.run(
    query[Day]
      .filter(d =>
        d.calendar == lift(calendar) &&
          d.date == lift(parseDate(date))
      )
      .map(d => !d.isMarkedHoliday)
  )
  */

  // Doesn't take weekends into consideration.
  def isMarkedHoliday(calendar: Int, date: String) = ctx.run(
      query[Day]
        .filter(d =>
          d.calendar == lift(calendar) &&
          d.date == lift(parseDate(date))
        )
        .map(d => d.isHoliday)
    )
  def insertDays(days: List[Day]) = ctx.run(liftQuery(days).foreach(d => query[Day].insert(d)))
  def getMarkedHolidays(calendar: Int, fromDate: String, toDate: String) = ctx.run(
      query[Day]
        .filter(d =>
          d.calendar == lift(calendar) &&
            d.isHoliday &&
            (d.date > lift(parseDate(fromDate)) || d.date == lift(parseDate(fromDate))) &&
            (d.date < lift(parseDate(toDate)) || d.date == lift(parseDate(toDate)))
        )
        .map(d => d.date)
  )
  def allDays = ctx.run(query[Day])
  def countDays = ctx.run(quote(query[Day]).size)
  def deleteOne(calendar: Int, date: String) = {
    ctx.run(
      query[Day]
        .filter( d =>
          d.calendar == lift(calendar) &&
          d.date == lift(parseDate(date))
        )
        .delete
    )
    this.countDays
  }
  def deleteAll = ctx.run(query[Day].delete)
}
