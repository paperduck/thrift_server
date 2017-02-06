package com.twitter.calculator

import com.twitter.calculator.thriftscala.{Calculator}
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class CalculatorServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedThriftServer(new CalculatorServer)

  val client = server.thriftClient[Calculator[Future]](clientId = "client123")

  // delete should delete all rows

  "InsertDay" should {
    "increase table's record count by one" in {
      val initialCount = Await.result(client.countDays())
      val res = Await.result(client.insertDay(thriftscala.Exchange.Jpx,"2017-01-01",false,false))
      val secondCount = Await.result(client.countDays())
      secondCount should equal(initialCount + 1)
    }
  }

  "IsHoliday" should {
    "return true then false" in{
      val delRes = Await.result(client.deleteAll())
      var res = Await.result(client.insertDay(thriftscala.Exchange.Jpx,"2017-01-01",true,false))
      var holidayRes = Await.result(client.isHoliday("2017-01-01"))
      holidayRes should equal(true)
      res = Await.result(client.insertDay(thriftscala.Exchange.Nasdaq, "2017-01-02",false,true))
      holidayRes = Await.result(client.isHoliday("2017-01-02"))
      holidayRes should equal(false)
    }
  }

  "whitelist clients" should {
    "be allowed" in {
      client.increment(1).value should equal(2)
      client.addNumbers(1, 2).value should equal(3)
      client.addStrings("1", "2").value should equal("3")
    }
  }

  "blacklist clients" should {
    "be blocked with UnknownClientIdException" in {
      val clientWithUnknownId = server.thriftClient[Calculator[Future]](clientId = "unlisted-client")
      intercept[UnknownClientIdError] { clientWithUnknownId.increment(2).value }
    }
  }

  "clients without a client-id" should {
    "be blocked with NoClientIdException" in {
      val clientWithoutId = server.thriftClient[Calculator[Future]]()
      intercept[NoClientIdError] { clientWithoutId.increment(1).value }
    }
  }
}
