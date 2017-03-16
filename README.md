# Calendar Server

## Background

This was created during a 2-week internship. I am no longer making changes to it. I obtained permission to make this public on my GitHub.

During this project I became acquainted with several things:
- Scala (and Java)
- [Futures](http://docs.scala-lang.org/overviews/core/futures.html) (asynchronous programming)
- Finagle, [Finatra](https://github.com/twitter/finatra), [Thrift](https://thrift.apache.org/) servers
- [Quill](https://github.com/getquill/quill) (object-oriented database queries for Scala)
- [Mustache](https://mustache.github.io/) (simple dynamic server pages)
- [MariaDB](https://mariadb.org/) (open source version of MySQL)
- [sbt](http://www.scala-sbt.org/index.html) (Scala Build Tool / Simple Build Tool) for unit/regression testing and building.

## Application Overview

The calendar server provides a way to mark and track days for future reference.

For example, you can mark days as holidays using a web GUI. You can enter a date and query when the next business day is. There are several other functions available.

The [web server](https://github.com/paperduck/thrift_server/tree/master/calendar-server-main/src/main/webapp) serves the GUI and uses the [HTTP controller](https://github.com/paperduck/thrift_server/blob/master/calendar-server-main/src/main/scala/com/twitter/calendar/CalendarAdminHttpController.scala).
The HTTP controller calls the [database layer](https://github.com/paperduck/thrift_server/blob/master/calendar-server-main/src/main/scala/com/twitter/calendar/db/DayService.scala) which uses Quill to access the MariaDB database.
The [Thrift server controller](https://github.com/paperduck/thrift_server/blob/master/calendar-server-main/src/main/scala/com/twitter/calendar/CalendarController.scala) provides a similar interface as the HTTP controller but returns data in the [Thrift IDL](https://thrift.apache.org/docs/idl) format should you need that for whatever reason.

## Achievements

- I submitted a bug report to Quill:
    https://github.com/getquill/quill/issues/708

- I used Scala for the first time. My previous experience working with Haskell helped me quickly adjust to using functional programming in a real-world application.

