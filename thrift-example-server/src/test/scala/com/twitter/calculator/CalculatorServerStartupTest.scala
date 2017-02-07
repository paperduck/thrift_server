package com.twitter.calculator

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.thrift.ThriftClient
import com.twitter.inject.server.FeatureTest

class CalculatorServerStartupTest extends FeatureTest {

  val server = new EmbeddedHttpServer(
    twitterServer = new CalculatorServer,
    stage = Stage.PRODUCTION) with ThriftClient

  "server" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}