package com.twitter.calendar

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTest

class CalendarServerStartupTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new CalendarServer,
    stage = Stage.PRODUCTION) with ThriftClient

  "server" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}