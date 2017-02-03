package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.calculator.thriftscala.Calculator._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}
import java.util.HashMap

import scala.concurrent.duration._

//import com.twitter.calculator.db.PersonService
import com.twitter.calculator.db._
import java.time.LocalDate
import scala.Enumeration


@Singleton
//class CalculatorController @Inject()(personService: PersonService)//PersonService is here just to demonstrate how to use service in controller
class CalculatorController @Inject()(dayService: DayService)
  extends Controller
  with Calculator.BaseServiceIface {

  override val isHoliday = handle(IsHoliday) { args: IsHoliday.Args =>
    val queryResult = dayService.isHoliday(LocalDate.ofEpochDay(args.epochDays))
    val result = queryResult.map {r => r(0).isHoliday} // not invoked until the integer value becomes available
    result
  }

  override val addDay = handle(AddDay) { args: AddDay.Args =>
    var success = true
    info(s"Adding day...")
    val newDay = Day(exchangeId=args.exchangeId, date=LocalDate.ofEpochDay(args.date),
      isHoliday=args.isHoliday, isBusinessDay=args.isBusinessDay)
    val queryResult = dayService.insertDays(List(newDay))
    Future.value(success)
    //queryResult.map({x => info("Finished"); x})
  }

  override val getHolidays = handle(GetHolidays) { args: GetHolidays.Args =>
    info(s"Getting holidays...")
    val queryResult = dayService.getHolidays(args.exchangeName, args.fromDate, args.toDate)
    val resultSet = queryResult.map {r => r(0)}  // Java ArrayList
    // transform result (ArrayList of Day) into idl-friendly type (ArrayList of HashMap)
    val idlRet = resultSet.map(d => val h = HashMap<
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
