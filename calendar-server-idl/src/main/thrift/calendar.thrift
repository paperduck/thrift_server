namespace java com.twitter.calendar.thriftjava
#@namespace scala com.twitter.calendar.thriftscala
namespace rb Calendar

include "finatra-thrift/finatra_thrift_exceptions.thrift"

typedef string LocalDate

enum CalendarEnum {
    JPX         = 0,
    Japannext   = 1,
    NASDAQ      = 2
}

struct Day {
  1:CalendarEnum calendar,
  2:LocalDate date,
  3:bool isHoliday,
}

service Calendar {

    LocalDate getNextBusinessDay(
        1: CalendarEnum calendar
        2: LocalDate startDate
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    bool isTodayBusinessDay(
        1: CalendarEnum calendar
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    bool isBusinessDay(
        1: CalendarEnum calendar
        2: LocalDate date
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /**
    * Returns true if a day is a holiday.
    */
    bool isHoliday(
        1: CalendarEnum calendar
        2: LocalDate date
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /**
    * Insert a day into the calendar.
    */
    bool InsertDay(
        1: CalendarEnum calendar
        2: LocalDate date
        3: bool isHoliday
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /**
    * Get holidays of a given calendar between two dates.
    */
    list<string> GetHolidays(
        1: CalendarEnum calendar
        2: string fromDate
        3: string toDate
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    i64 deleteOne(
      1: CalendarEnum calendar
      2: LocalDate date
    ) throws (
        1: finatra_thrift_exceptions.ServerError  serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError  noClientIdError
    )

    i64 countDays(
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    i64 deleteAll(
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

  /************************************************************************/

  /**
   * Increment a number
   */
  i32 increment(
    1: i32 a
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  /**
   * Add two numbers
   */
  i32 addNumbers(
    1: i32 a
    2: i32 b
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  /**
   * Add two strings
   */
  string addStrings(
    1: string a
    2: string b
  ) throws (
    1: finatra_thrift_exceptions.ClientError clientError,
    2: finatra_thrift_exceptions.ServerError serverError,
    3: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    4: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )
}
