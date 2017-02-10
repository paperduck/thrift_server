package com.twitter.calendar

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalDate, ZoneOffset}

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
      initialCount shouldBe a [java.lang.Long]
      val insertResult = Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-01-01", false))
      insertResult shouldBe a [java.lang.Boolean]
      val secondCount = Await.result(client.countDays())
      secondCount should equal(initialCount + 1)
    }
  }

  // database PRIMARY KEY constraint on (calendar, date)
  "Failed database insert" should {
    "throw mysql.ServerError when existing (calendar, date) exists." in {
      Await.result(client.deleteAll())
      val dayList = List(
        // Try adding two days with same 'calendar' and 'date', but different 'isHoliday'
        Day(CalendarEnum.fromThriftCalendarToDb(thriftscala.CalendarEnum.Jpx), parseDate("2017-03-02"), false),
        Day(CalendarEnum.fromThriftCalendarToDb(thriftscala.CalendarEnum.Jpx), parseDate("2017-03-02"), true)
      )
      val service = injector.instance[DayService]
      an [mysql.ServerError] should be thrownBy service.insertDays(dayList).value
    }
  }

  "IsHoliday" should {
    "be true on the weekend, true on weekday marked as holiday, and false on non-holiday weekday" in{
      Await.result(client.deleteAll())

      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, "2017-01-01", false))
      var result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Jpx, "2017-01-01"))
      result shouldBe a [java.lang.Boolean]
      result should equal(true)

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-02", true))
      result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-02"))
      result shouldBe a [java.lang.Boolean]
      result should equal(true)

      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Nasdaq, "2017-01-03", false))
      result = Await.result(client.isHoliday(thriftscala.CalendarEnum.Nasdaq, "2017-01-03"))
      result shouldBe a [java.lang.Boolean]
      result should equal(false)
    }
  }

  "isHoliday called with invalid date string" should {
    "throw the correct Exception" in {
      val query = client.isHoliday(thriftscala.CalendarEnum.Jpx, "abcdef")
      an[com.twitter.finatra.thrift.thriftscala.ServerError] should be thrownBy query.value
    }
  }

  "IsTodayBusinessDay" should {
    "be false if it's the weekend, and false if marked as holiday" in {
      val today = LocalDate.now(ZoneOffset.UTC)
      val isWeekend = List(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(today.getDayOfWeek())

      // empty database
      Await.result(client.deleteAll())
      var result = Await.result(client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx))
      result shouldBe a [java.lang.Boolean]
      result should be (isWeekend)
      // with a day marked as non-holiday
      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, serializeDate(today), false))
      result = Await.result(client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx))
      result shouldBe a [java.lang.Boolean]
      if (isWeekend) {
        result should be (false)
      }else{
        result should be  (true)
      }
      // with a day marked as a holiday
      Await.result(client.deleteAll())
      Await.result(client.insertDay(thriftscala.CalendarEnum.Jpx, serializeDate(today), true))
      result = Await.result(client.isTodayBusinessDay(thriftscala.CalendarEnum.Jpx))
      result shouldBe a [java.lang.Boolean]
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

  "GetNextBusinessDay with invalid date string" should {
    "throw the correct error when given a date string that can't be parssed" in {
      val result = client.getNextBusinessDay(thriftscala.CalendarEnum.Nasdaq, "abcde")
      an [com.twitter.finatra.thrift.thriftscala.ServerError] should be thrownBy result.value
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
      val delResult = Await.result(client.deleteOne(thriftscala.CalendarEnum.Japannext, "2017-02-06"))
      delResult shouldBe a [java.lang.Long]
      //delResult should be (1) // MariaDB DELETE returns number of deleted rows
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
