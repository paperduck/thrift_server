package com.twitter.calculator.db

import java.time.LocalDate
import java.util.Date
import javax.inject.{Inject, Singleton}
import java.time.format.DateTimeFormatter

import com.twitter.calculator.{CalendarEnum, thriftscala}
import io.getquill.context.Context
import io.getquill.context.sql.SqlContext
import io.getquill.{FinagleMysqlContext, Literal}

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
    //def toEpoch = quote(infix"DATEDIFF($ldt,'1970-01-01')".as[Long])
  }

  def now = quote(infix"now()".as[LocalDate])
  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)
  implicit val encodeCalendarEnum = MappedEncoding[CalendarEnum, Int](_.int)
  implicit val decodeCalendarEnum = MappedEncoding[Int, CalendarEnum](CalendarEnum.fromInt)

  // Doesn't take weekends into consideration.
  def isBusinessDay(calendar: Int, date: String) = ctx.run(
    query[Day]
      .filter(d =>
        d.calendar == lift(calendar) &&
          d.date == lift(parseDate(date))
      )
      .map(d => !d.isHoliday)
  )

  // Doesn't take weekends into consideration.
  def isHoliday(calendar: Int, date: String) = ctx.run(
      query[Day]
        .filter(d =>
          d.calendar == lift(calendar) &&
          d.date == lift(parseDate(date))
        )
        .map(d => d.isHoliday)
    )
  def insertDays(days: List[Day]) = ctx.run(liftQuery(days).foreach(d => query[Day].insert(d)))
  def getHolidays(calendar: Int, fromDate: String, toDate: String) = ctx.run(
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
