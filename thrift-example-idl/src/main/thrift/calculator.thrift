namespace java com.twitter.calculator.thriftjava
#@namespace scala com.twitter.calculator.thriftscala
namespace rb Calculator

include "finatra-thrift/finatra_thrift_exceptions.thrift"

typedef string LocalDate

enum Exchange {
    JPX         = 0,
    Japannext   = 1,
    NASDAQ      = 2
}

struct Day {
  1:Exchange exchange,
  2:LocalDate date,
  3:bool isHoliday,
  4:bool isBusinessDay
}

service Calculator {

  /**
   * Returns true if a day is a holiday.
   */
  bool isHoliday(
    2: LocalDate date
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

    /**
    * Insert a day into the calendar.
    * Day is assumed to be days since Epoch.
    */
    bool InsertDay(
        1: Exchange exchange
        2: LocalDate date
        3: bool isHoliday
        4: bool isBusinessDay
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /**
    * Get holidays of an exchange between two dates.
    * Return example: [ map(exchangeId, date), map(isHoliday, isBusinessDay) ]
    */
    list<string> GetHolidays(
        1: Exchange exchange
        2: string fromDate
        3: string toDate
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /*
    *
    */
    i64 countDays(
    ) throws (
        1: finatra_thrift_exceptions.ServerError serverError,
        2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
        3: finatra_thrift_exceptions.NoClientIdError noClientIdError
    )

    /*
    *
    */
    delete
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
