package com.twitter.calculator.example

import com.twitter.calculator.thriftscala.{Calculator, Calculator$FinagleClient}
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
  println("Calling addDay on remote thrift server: " + remoteServer + "...")
  var res = Await.result(client.addDay(0,99,false,false))
  println("Result is " + res)

  // isHoliday
  println("Calling isHoliday")
  res = Await.result(client.isHoliday(88))
  println(s"epoch(88) is a holiday?  $res")
  res = Await.result(client.isHoliday(1))
  println(s"epoch(1) is a holiday?  $res")

  // getHolidays
  println("Calling getHolidays")
  res = Await.result(client.getHolidays("JPX", 0, 1000))
  println(s"Result is $res")
  client.asInstanceOf[Calculator$FinagleClient].service.close()
}
