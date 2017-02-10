package com.twitter.calendar.example

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.twitter.calendar.thriftscala.{Calendar, Calendar$FinagleClient, CalendarEnum}
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.ClientId
import com.twitter.util.{Await, Future}

object CalendarClientExample extends App {

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)

  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  val remoteServer = "localhost:9911"
  val client = ThriftMux.Client()
    .withClientId(ClientId("client123"))
    .newIface[Calendar[Future]](remoteServer, "calculator-server")

  // addDay
  Await.result(client.deleteAll())
  println("Calling insertDay on remote thrift server: " + remoteServer + "...")
  var res = Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-01", false))
  println(s" Inserted:  JPX | 2017-01-01 | false")
  res = Await.result(client.insertDay(CalendarEnum.Japannext, "2017-01-02", false))
  println(s" Inserted:  Japannext | 2017-01-02 | false")
  res = Await.result(client.insertDay(CalendarEnum.Nasdaq, "2017-01-03", true))
  println(s" Inserted:  NASDAQ | 2017-01-03 | true")
  res = Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-04", true))
  println(s" Inserted:  JPX | 2017-01-04 | true")
  println("")

  // deleteOne(calendar, date)
  println("Calling deleteOne")
  var dayCount = Await.result(client.countDays())
  println(s"  There are $dayCount days")
  println("  Deleting  JPX | 2017-01-01")
  Await.result(client.deleteOne(CalendarEnum.Jpx, "2017-01-01"))
  dayCount = Await.result(client.countDays())
  println(s"  There are $dayCount days")
  println("")

  // isHoliday
  println("Calling isHoliday")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-01", true))
  println(s"  Inserted:  JPX | 2017-01-01 | true")
  res = Await.result(client.isHoliday(CalendarEnum.Japannext, "2017-01-01"))
  println(s"    JPX | 2017-01-01  is a holiday (or weekend)?  $res")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-01-02", false))
  println(s"  Inserted:  JPX | 2017-01-02 | false")
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-01-02"))
  println(s"    2017-01-02 is a holiday (or weekend)?  $res")
  Await.result(client.insertDay(CalendarEnum.Jpx, "2017-02-05", false))
  println(s"  Inserted:  JPX | 2017-02-05 | false")
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-02-05"))
  println(s"    2017-02-05 is a holiday (or weekend)?  $res")
  res = Await.result(client.isHoliday(CalendarEnum.Nasdaq, "2017-02-26"))
  println(s"    2017-02-26 (SUN) is a holiday (or weekend)?  $res")
  println("")

  // getMarkedHolidays
  println("Calling getMarkedHolidays")
  val fromDate = "2016-01-01"
  val toDate = "2017-12-01"
  var holidayList = Await.result(client.getMarkedHolidays(CalendarEnum.Jpx, fromDate, toDate))
  if (holidayList.length == 0){
    println(s"No holidays with those constraints.")
  }
  else {
    println(s"  The holidays from $fromDate to $toDate are:")
    for (h <- holidayList) {
      println(s"  -> $h")
    }
  }
  println("")

  // isBusinessDay
  println("Calling isBusinessDay")
  println("  Deleting all days...")
  Await.result(client.deleteAll())
  var isBus = Await.result(client.isBusinessDay(CalendarEnum.Nasdaq, "2017-02-26"))
  println(s"  Is (2017-02-26)(SUN) a business day? $isBus")
  isBus = Await.result(client.isBusinessDay(CalendarEnum.Nasdaq, "2017-02-27"))
  println(s"  Is (2017-02-27)(MON) a business day? $isBus")
  val weekdayHoliday = "2017-03-01"
  Await.result(client.insertDay(CalendarEnum.Jpx, weekdayHoliday, false))
  println(s" Inserted:  JPX | $weekdayHoliday  | false")
  println(s"  Is ($weekdayHoliday) a business day? $isBus")
  println("")

  // getNextBusinessDay
  println("Calling getNextBusinessDay")
  Await.result(client.deleteAll())
  var tempDay = "2017-02-03"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, true))
  println(s" Inserted:  NASDAQ | $tempDay | true")
  tempDay = "2017-02-04"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, false))
  println(s" Inserted:  NASDAQ | $tempDay | false")
  tempDay = "2017-02-05"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, true))
  println(s" Inserted:  NASDAQ | $tempDay | true")
  tempDay = "2017-02-06"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, true))
  println(s" Inserted:  NASDAQ | $tempDay | true")
  tempDay = "2017-02-07"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, false))
  println(s" Inserted:  NASDAQ | $tempDay | false")
  tempDay = "2017-02-08"
  Await.result(client.insertDay(CalendarEnum.Nasdaq, tempDay, true))
  println(s" Inserted:  NASDAQ | $tempDay | true")
  tempDay = "2017-02-02"
  val nextBusDay = Await.result(client.getNextBusinessDay(CalendarEnum.Nasdaq, tempDay))
  println(s"Next business day after $tempDay is $nextBusDay")
  println("")


  client.asInstanceOf[Calendar$FinagleClient].service.close()
}
