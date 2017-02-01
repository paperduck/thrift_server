package com.twitter.calculator.db

import java.time.LocalDate

case class Day (date: LocalDate, isHoliday: Boolean, isBusinessDay: Boolean)
