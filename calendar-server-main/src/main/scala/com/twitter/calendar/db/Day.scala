package com.twitter.calendar.db

import java.time.LocalDate

case class Day (calendar: Int, date: LocalDate, isHoliday: Boolean)
