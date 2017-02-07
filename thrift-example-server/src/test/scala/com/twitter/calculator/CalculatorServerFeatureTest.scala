package com.twitter.calculator

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import com.twitter.calculator.thriftscala.Calendar
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class CalculatorServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new CalculatorServer) with ThriftClient

  val client = server.thriftClient[Calendar[Future]](clientId = "client123")

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  "InsertDay" should {
    "increase table's record count by one" in {
      val initialCount = Await.result(client.countDays())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx,"2017-01-01",false))
      val secondCount = Await.result(client.countDays())
      secondCount should equal(initialCount + 1)
    }
  }

  "IsHoliday" should {
    "be true on the weekend, true on weekday marked as holiday, and false on non-holiday weekday" in{
      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-01-01", false))
      var result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Jpx, "2017-01-01"))
      result should equal(true)

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-02", true))
      result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-02"))
      result should equal(true)

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-03", false))
      result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-03"))
      result should equal(false)
    }
  }

  "IsBusinessDay" should {
    "be false if it's the weekend, and false if marked as holiday" in {
      val today = serializeDate(LocalDate.now(ZoneOffset.UTC))

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, today, false))
      var result = Await.result(client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx))
      result should equal(true)

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, today, true))
      result = Await.result(client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx))
      result should equal(false)
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
      val clientWithUnknownId = server.thriftClient[Calendar[Future]](clientId = "unlisted-client")
      intercept[UnknownClientIdError] { clientWithUnknownId.increment(2).value }
    }
  }

  "clients without a client-id" should {
    "be blocked with NoClientIdException" in {
      val clientWithoutId = server.thriftClient[Calendar[Future]]()
      intercept[NoClientIdError] { clientWithoutId.increment(1).value }
    }
  }

  "http" should {
    "ping" in {
      server.httpGet("/ping", andExpect = Status.Ok, withBody = "pong")
    }
  }
}
