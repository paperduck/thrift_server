package com.twitter.calculator.db

import javax.inject.{Inject, Singleton}
import io.getquill.{FinagleMysqlContext, Literal}

trait PersonSchema {
  val ctx: FinagleMysqlContext[Literal]

  import ctx._
  def insert(persons: List[Person]) = quote {liftQuery(persons).foreach(p => query[Person].insert(p))}
}

@Singleton
class PersonService @Inject()(override val ctx: FinagleMysqlContext[Literal]) extends PersonSchema {

  import ctx._

  def insertPersons(persons: List[Person]) = run(insert(persons))
  def findPerson = ctx.run(query[Person])
  def deletePersons = ctx.run(query[Person].delete)
}
