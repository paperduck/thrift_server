package com.twitter.calculator.db

import java.time.LocalDate

case class Day (calendar: Int, date: LocalDate, isHoliday: Boolean)
