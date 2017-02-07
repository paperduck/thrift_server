package com.twitter.calculator

import javax.inject.Inject

import com.twitter.calculator.db.DayService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class CalendarAdminHttpController @Inject()(
  dayService: DayService
) extends Controller {

  get("/ping") { request: Request =>
    "pong"
  }
}