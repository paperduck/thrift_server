package com.twitter.calculator


import com.twitter.calculator.thriftscala.Calculator
import com.twitter.calculator.thriftscala.Calculator._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import com.twitter.calculator.db._

//import com.twitter.calculator.db.PersonService

//import scala.concurrent.duration._
//import scala.Enumeration
//import scala.collection.mutable.HashMap
import scala.collection.mutable.MutableList
//import scala.collection.mutable.Map
import scala.collection.JavaConverters._
import scala.collection.immutable.Map

import javax.inject.{Inject, Singleton}
import java.time.LocalDate
import java.util.{HashMap}
import java.time.format.DateTimeFormatter



@Singleton
//class CalculatorController @Inject()(personService: PersonService)//PersonService is here just to demonstrate how to use service in controller
class CalculatorController @Inject()(dayService: DayService)
  extends Controller
  with Calculator.BaseServiceIface {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  override val isHoliday = handle(IsHoliday) { args: IsHoliday.Args =>
    val queryResult = dayService.isHoliday(args.date)
    val result = queryResult.map {r => r(0).isHoliday} // not invoked until the integer value becomes available
    result
  }

  override val insertDay = handle(InsertDay) { args: InsertDay.Args =>
    var success = true
    info(s"Adding day...")
    val newDay = Day(exchangeId=args.exchange.asInstanceOf[Int], date=parseDate(args.date),
      isHoliday=args.isHoliday, isBusinessDay=args.isBusinessDay)
    val queryResult = dayService.insertDays(List(newDay))
    Future.value(success)
    //queryResult.map({x => info("Finished"); x})
  }

  override val getHolidays = handle(GetHolidays) { args: GetHolidays.Args =>
    info(s"Getting holidays...")
    val queryResult = dayService.getHolidays(args.exchange.asInstanceOf[Int], args.fromDate, args.toDate)
    //queryResult.map {r => r(0)}  // extract from Future
    // date comes from db as date; convert to string
    queryResult.map{row => row.map(elem => serializeDate(elem))}
  }

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
