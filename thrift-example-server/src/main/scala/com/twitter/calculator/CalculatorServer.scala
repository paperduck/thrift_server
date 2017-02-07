package com.twitter.calculator

import com.twitter.calculator.db.QuillDbContextModule
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends HttpServer with ThriftServer {
  override val name = "calculator-server"
  override val defaultFinatraHttpPort: String = ":9910"
  override val defaultFinatraThriftPort: String = ":9911"
  override def defaultHttpPort: Int = 9912 //server admin port

  override def modules = Seq(
    ClientIdWhitelistModule,
    QuillDbContextModule) //module for quill

  override def configureThrift(router: ThriftRouter) {
    router
      .filter[thrift.filters.LoggingMDCFilter]
      .filter[thrift.filters.TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[CalculatorController]
  }

  override protected def configureHttp(router: HttpRouter) = {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter(new Cors.HttpFilter(Cors.UnsafePermissivePolicy))
      //.filter[ExceptionMappingFilter[Request]] //already in common filters
      .add[CalendarAdminHttpController]
  }
}
