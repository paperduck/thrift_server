package com.twitter.calculator.db

import java.time.LocalDate

case class Day (exchange: Int, date: LocalDate, isHoliday: Boolean, isBusinessDay: Boolean)
