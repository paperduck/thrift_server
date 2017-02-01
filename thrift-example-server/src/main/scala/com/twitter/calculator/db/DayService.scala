package com.twitter.calculator.db

import javax.inject.{Inject, Singleton}
import io.getquill.{FinagleMysqlContext, Literal}
import java.time.LocalDate


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

trait DaySchema {
  val ctx: FinagleMysqlContext[Literal]

  import ctx._

  def insertDays_(days: List[Day]) = quote {liftQuery(days).foreach(d => query[Day].insert(d))}
  def getHolidays_(exchangeId: Int, toDate: LocalDate, fromDate: LocalDate) = quote{
    query[Day].filter(d => d.isHoliday == true)
      .filter(d => d.exchangeId == exchangeId)
      .filter(d => d.date >= fromDate)
      .filter(d => d.date <= toDate)
      .map(d => d.date)
  }
}

@Singleton
class DayService @Inject()(override val ctx: FinagleMysqlContext[Literal]) extends DaySchema {

  import ctx._

  implicit val encodeExchange = MappedEncoding[Exchange, Int](_.int)
  implicit val decodeExchange = MappedEncoding[Int, Exchange](Exchange.fromInt)

  def insertDays(days: List[Day]) = run(insertDays_(days))
  def getHolidays(exchange: Exchange, toDate: LocalDate, fromDate: LocalDate) = run(getHolidays_(exchange.int, toDate, fromDate))

  def findDay = ctx.run(query[Day])
  def deleteDays = ctx.run(query[Day].delete)

}
