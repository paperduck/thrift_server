package com.twitter.calculator.db

import java.time.{LocalDate}
import java.util.Date
import javax.inject.{Inject, Singleton}

import io.getquill.context.Context
import io.getquill.context.sql.SqlContext
import io.getquill.{FinagleMysqlContext, Literal}


sealed trait Exchange {
  def value: String
  def int: Int
}


object Exchange {
  case object JPX       extends Exchange {val value = "JPX";        val int = 0}
  case object Japannext extends Exchange {val value = "Japannext";  val int = 1}
  case object NASDAQ    extends Exchange {val value = "NASDAQ";     val int = 2}

  def fromString(value: String): Exchange = value match{
    case Exchange.JPX.value       => Exchange.JPX
    case Exchange.Japannext.value => Exchange.Japannext
    case Exchange.NASDAQ.value    => Exchange.NASDAQ
  }

  def fromInt(value: Int): Exchange = value match{
    case Exchange.JPX.int       => Exchange.JPX
    case Exchange.Japannext.int => Exchange.Japannext
    case Exchange.NASDAQ.int    => Exchange.NASDAQ
  }
}

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
  def now = quote(infix"now()".as[LocalDate])

  implicit val encodeExchange = MappedEncoding[Exchange, Int](_.int)
  implicit val decodeExchange = MappedEncoding[Int, Exchange](Exchange.fromInt)

  def isHoliday(date: LocalDate) = {
    val queryResult = ctx.run(query[Day].filter(d => d.date == lift(date)))
    queryResult
  }
  def insertDays(days: List[Day]) = ctx.run(liftQuery(days).foreach(d => query[Day].insert(d)))
  def getHolidays(exchangeName: String, toDate: Int, fromDate: Int) = ctx.run(
      query[Day]
        .filter(d =>
          d.exchangeId == lift(Exchange.fromString(exchangeName).int) &&
            d.isHoliday &&
            (d.date > lift(LocalDate.ofEpochDay(fromDate)) || d.date == lift(LocalDate.ofEpochDay(fromDate))) &&
            (d.date < lift(LocalDate.ofEpochDay(toDate)) || d.date == lift(LocalDate.ofEpochDay(toDate)))
        )
  )
  //def findDay = ctx.run(query[Day])
  def findDay = ctx.run(query[Day])
  //def deleteDays = ctx.run(query[Day].delete)
  def deleteDays = ctx.run(query[Day].delete)
}
