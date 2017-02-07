package com.twitter.calculator.example

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.twitter.calculator.thriftscala.{Calendar, Calculator$FinagleClient, CalendarEnum}
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.ClientId
import com.twitter.util.{Await, Future}

object CalculatorClientExample extends App {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)

  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  val remoteServer = "localhost:9911"
  val client = ThriftMux.Client()
    .withClientId(ClientId("client123"))
    .newIface[Calendar[Future]](remoteServer, "calculator-server")

  /*
  println("Calling addNumbers on remote thrift server: " + remoteServer + "...")
  val res = Await.result(client.addNumbers(1, 2))
  println("Result is " + res)
  */

  // addDay
  println("Calling insertDay on remote thrift server: " + remoteServer + "...")
  var res = Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-01", false))
  println(s"  2017-01-01")
  res = Await.result(client.insertDay(CalendarEnum.Japannext, "2017-01-02", false))
  println(s"  2017-01-02")
  res = Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-01-03", true))
  println(s"  2017-01-03")
  res = Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-04", true))
  println(s"  2017-01-04")

  // isHoliday
  println("Calling isHoliday")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-01", true))
  res = Await.result(client.isHoliday(CalendarEnum.Japannext, "2017-01-01"))
  println(s"  2017-01-01 is a holiday?  $res")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-02", false))
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-01-02"))
  println(s"  2017-01-02 is a holiday?  $res")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-02-05", false))
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-02-05"))
  println(s"  2017-02-05 is a holiday?  $res")
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-02-26"))
  println(s"  2017-02-26 is a holiday?  $res")

  // getHolidays
  println("Calling getHolidays")
  var holidayList = Await.result(client.getHolidays(CalendarEnum.Jpx, "2016-01-01", "2017-12-01"))
  if (holidayList.length == 0){
    println(s"No holidays with those constraints.")
  }
  else {
    println(s"The holidays are:")
    for (h <- holidayList) {
      println(s"  -> $h")
    }
  }

  // isBusinessDay
  println("Calling isBusinessDay")
  Await.result(client.deleteAll())
  var isBus = Await.result(client.isBusinessDay(CalendarEnum.Nasdaq, "2017-02-26"))
  println(s"Is (2017-02-26)(SUN) a business day? $isBus")
  isBus = Await.result(client.isBusinessDay(CalendarEnum.Nasdaq, "2017-02-27"))
  println(s"Is (2017-02-27)(MON) a business day? $isBus")

  // getNextBusinessDay
  println("Calling getNextBusinessDay")
  Await.result(client.deleteAll())
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-03", true))
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-04", false))
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-05", true))
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-06", true))
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-07", false))
  Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-02-08", true))
  val nextBusDay = Await.result(client.getNextBusinessDay(CalendarEnum.Nasdaq, "2017-02-03"))
  println(s"Next business day: $nextBusDay")

  client.asInstanceOf[Calculator$FinagleClient].service.close()
}
