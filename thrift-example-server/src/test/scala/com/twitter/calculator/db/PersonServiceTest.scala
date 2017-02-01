package com.twitter.calculator.db

import com.twitter.inject.app.TestInjector
import com.twitter.util.Await
import org.scalatest.{Matchers, WordSpec}

class PersonServiceTest extends WordSpec with Matchers{

  "person service" should {
    "delete, insert, and find person" in {
      val inj = TestInjector(modules = Seq(QuillDbContextModule))
      val service = inj.instance[PersonService]

      //clear old data first
      Await.result(service.deletePersons)

      val data = List(Person(1, "alice"), Person(2, "bob"))
      val ins = Await.result(service.insertPersons(data))
      ins shouldBe List(1, 1)

      val persons = Await.result(service.findPerson)
      persons shouldBe data
    }
  }

}
