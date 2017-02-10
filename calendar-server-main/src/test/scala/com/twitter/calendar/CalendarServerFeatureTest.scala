package com.twitter.calendar

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import com.twitter.calendar.db.{Day, DayService}
import com.twitter.calendar.thriftscala.Calendar
import com.twitter.finagle.http.Status
import com.twitter.finagle.mysql
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.finatra.thrift.thriftscala.{NoClientIdError, UnknownClientIdError}
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class CalendarServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new CalendarServer) with ThriftClient

  val client = server.thriftClient[Calendar[Future]](clientId = "client123")

  def serializeDate(ld: LocalDate): String = ld.format(DateTimeFormatter.ISO_LOCAL_DATE)
  def parseDate(ldStr: String): LocalDate = LocalDate.parse(ldStr, DateTimeFormatter.ISO_LOCAL_DATE)

  "InsertDay" should {
    "increase table's record count by one" in {
      Await.result(client.deleteAll())
      val initialCount = Await.result(client.countDays())
      client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-01-01", false)
      val secondCount = Await.result(client.countDays())
      secondCount should equal(initialCount + 1)
    }
  }

  // database PRIMARY KEY constraint on (calendar, date)
  "Return value of failed insert" should {
    "be mysql.ServerError" in {
      Await.result(client.deleteAll())
      val dayList = List(
        Day(CalendarEnum.fromThriftCalendarToDb(thriftscala.CalendarEnum.Jpx), parseDate("2017-03-02"), false),
        Day(CalendarEnum.fromThriftCalendarToDb(thriftscala.CalendarEnum.Jpx), parseDate("2017-03-02"), true)
      )
      val service = injector.instance[DayService]
      an[mysql.ServerError] should be thrownBy service.insertDays(dayList).value
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

  "GetNextBusinessDay" should {
    "return the correct business day" in {
      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-03", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-04", false))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-05", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-06", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-07", false))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-02-08", true))
      val result = Await.result(client.getNextBusinessDay(thriftscala.CalendarEnum.Jpx, "2017-02-03"))
      result should equal ("2017-02-07")
    }
  }

  "DeleteOne" should {
    "reduce the count by one" in {
      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-03", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-04", false))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-05", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-06", true))
      Await.result(client.insertDay(thriftscala.CalendarEnum.Japannext, "2017-02-07", false))
      val initialCount = Await.result(client.countDays())
      Await.result(client.deleteOne(thriftscala.CalendarEnum.Japannext, "2017-02-06"))
      val secondCount = Await.result(client.countDays())
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
    "/" in {
      server.httpGet("/", andExpect = Status.Ok)
    }
  }
}
