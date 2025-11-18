package com.sksamuel.kotest.parallelism

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec

@Ignored
class TestSpec1 : StringSpec({
   "test 1" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec2 : StringSpec({
   "test 2" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec3 : StringSpec({
   "test 3" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec4 : StringSpec({
   "test 4" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec5 : StringSpec({
   "test 5" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec6 : StringSpec({
   "test 6" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec7 : StringSpec({
   "test 7" {
      startAndLockTest()
   }
})

@Ignored
class TestSpec8 : StringSpec({
   "test 8" {
      startAndLockTest()
   }
})
