namespace java com.twitter.calculator.thriftjava
#@namespace scala com.twitter.calculator.thriftscala
namespace rb Calculator

include "finatra-thrift/finatra_thrift_exceptions.thrift"

service Calculator {

  /**
   * Insert a day into the calendar.
   * Day is assumed to be days since Epoch.
   */
  bool AddDay(
    1: i32 d
  ) throws (
    1: finatra_thrift_exceptions.ServerError serverError,
    2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
    3: finatra_thrift_exceptions.NoClientIdError noClientIdError
  )

  /**
   *
   */
  bool GetHolidays(
    1: string exchange
    2: i32 fromDate
    3: i32 toDate
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
