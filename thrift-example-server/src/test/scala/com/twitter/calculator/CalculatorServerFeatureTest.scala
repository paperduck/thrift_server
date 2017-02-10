package com.twitter.calculator

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import com.twitter.calculator.thriftscala.Calendar
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import org.scalatest.BeforeAndAfterEach

class CalculatorServerFeatureTest extends FeatureTest with BeforeAndAfterEach {

  override val server = new EmbeddedHttpServer(new CalculatorServer, disableTestLogging = true) with ThriftClient

  lazy val client = server.thriftClient[Calendar[Future]](clientId = "client123")

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  override def beforeEach(): Unit = {
    super.beforeEach()
    client.deleteAll().value
  }

  "InsertDay" should {
    "increase table's record count by one" in {
      val initialCount = client.countDays().value
      client.insertDay(thriftscala.CalendarEnum.Jpx,"2017-01-01",false).value
      val secondCount = client.countDays().value
      secondCount should equal(initialCount + 1)
    }
  }

  "IsHoliday" should {
    "be true on the weekend, true on weekday marked as holiday, and false on non-holiday weekday" in{
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-01-01", false).value
      var result = client.isHoliday(thriftscala.CalendarEnum.Jpx, "2017-01-01").value
      result should equal(true)

      client.deleteAll().value
      client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-02", true).value
      result = client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-02").value
      result should equal(true)

      client.deleteAll().value
      client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-03", false).value
      result = client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-03").value
      result should equal(false)
    }
  }

  "IsBusinessDay" should {
    "be false if it's the weekend, and false if marked as holiday" in {
      val today = serializeDate(LocalDate.now(ZoneOffset.UTC))

      client.insertDay(thriftscala.CalendarEnum.Jpx, today, false).value
      var result = client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx).value
      result should equal(true)

      client.deleteAll().value
      client.insertDay(thriftscala.CalendarEnum.Jpx, today, true).value
      result = client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx).value
      result should equal(false)
    }
    "true when no day" in {
      val today = "2017-02-10"
      val result = client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx).value
      result should equal(true)
    }
    "false when day exists and is holiday" in {
      val today = "2017-02-10"
      client.insertDay(thriftscala.CalendarEnum.Jpx, today, true).value
      val result = client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx).value
      result should equal(false)
    }
  }

  "GetNextBusinessDay" should {
    "return the correct business day" in {
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-03", true).value
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-04", false).value
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-05", true).value
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-06", true).value
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-07", false).value
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-08", true).value
      val result = client.getNextBusinessDay(thriftscala.CalendarEnum.Jpx, "2017-02-03").value
      result should equal ("2017-02-07")
    }

    "skip weekend" in {

    }
  }

  "DeleteOne" should {
    "reduce the count by one" in {
      client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-03", true).value
      client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-04", false).value
      client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-05", true).value
      client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-06", true).value
      client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-07", false).value
      val initialCount = client.countDays().value
      client.deleteOne(thriftscala.CalendarEnum.Japannext, "2017-02-06").value
      val secondCount = client.countDays().value
      initialCount should equal(secondCount + 1)
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

  "http route" should {
    "/ping" in {
      server.httpGet("/ping", andExpect = Status.Ok, withBody = "pong")
    }
    "/" in {
      server.httpGet("/", andExpect = Status.Ok)
    }
    "/person" in {
      server.httpGet("/person", andExpect = Status.Ok)
    }
  }
}
