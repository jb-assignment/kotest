package io.kotest.assertions.ktor.client

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewSpec : FunSpec() {
   init {
       test("test 1") {
          1 shouldBe 1
       }

       test("test 2") {
          1 shouldBe 1
       }
   }
}
