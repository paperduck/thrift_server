package com.twitter.calculator.example

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.finagle.ThriftMux
import com.twitter.finagle.thrift.ClientId
import com.twitter.util.{Await, Future}

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

  println("Calling addDay on remote thrift server: " + remoteServer + "...")
  val res = Await.result(client.addDay(1))
  println("Result is " + res)
}
