package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.calculator.thriftscala.Calculator._
import com.twitter.finatra.thrift.Controller
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

//import com.twitter.calculator.db.PersonService
import com.twitter.calculator.db._
import java.time.LocalDate
import scala.Enumeration




@Singleton
//class CalculatorController @Inject()(personService: PersonService)//PersonService is here just to demonstrate how to use service in controller
class CalculatorController @Inject()(dayService: DayService)
  extends Controller
  with Calculator.BaseServiceIface {

  override val addDay = handle(AddDay) { args: AddDay.Args =>
    var ret = false
    info(s"Adding day...")
    val newDay = Day(date = LocalDate.ofEpochDay(args.d), isHoliday = true, isBusinessDay = false)
    val queryResult = dayService.insertDays(List(newDay))
    info(s"Finished adding day.")
    Future.value(ret)
  }

  override val getHolidays = handle(GetHolidays) { args: GetHolidays.Args =>
    var ret = false
    info(s"Getting holidays...")
    val queryResult = dayService.getHolidays()
    Future.value(ret)
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
