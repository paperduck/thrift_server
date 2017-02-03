package com.twitter.calculator.example

import com.twitter.calculator.thriftscala.{Calculator, Calculator$FinagleClient, Exchange}
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.ClientId
import com.twitter.util.{Await, Future}
import java.time.LocalDate

object CalculatorClientExample extends App {
  val remoteServer = "localhost:9911"
  val client = ThriftMux.Client()
    .withClientId(ClientId("client123"))
    .newIface[Calculator[Future]](remoteServer, "calculator-server")

  /*
  println("Calling addNumbers on remote thrift server: " + remoteServer + "...")
  val res = Await.result(client.addNumbers(1, 2))
  println("Result is " + res)
  */

  // addDay
  println("Calling insertDay on remote thrift server: " + remoteServer + "...")
  var res = Await.result(client.insertDay(Exchange.Jpx,"2017-01-05",false,false))
  println("Result is " + res)

  // isHoliday
  println("Calling isHoliday")
  res = Await.result(client.isHoliday("2017-01-08"))
  println(s"epoch(88) is a holiday?  $res")
  res = Await.result(client.isHoliday("2017-01-02"))
  println(s"epoch(1) is a holiday?  $res")

  // getHolidays
  println("Calling getHolidays")
  var resList = Await.result(client.getHolidays(Exchange.Japannext, "2016-01-01", "2017-12-01"))
  println(s"Result is $resList")
  client.asInstanceOf[Calculator$FinagleClient].service.close()
}
