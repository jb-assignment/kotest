import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAll
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class TestGrouperTest {

    @Test
    fun `should put tests from the same class into the same batch`() {
        // given
        val numberOfBatches = 2

        val firstTest = testResult("first test", "com.example.SomeClass")
        val secondTest = testResult("second test", "com.example.SomeClass")
        val thirdTest = testResult("third test", "com.example.AnotherClass")
        val testResults = listOf(firstTest, secondTest, thirdTest)

        // when
        val batches = TestGrouper.groupIntoBatches(numberOfBatches, testResults)

        // then
        batches shouldHaveSize 2
        batches.shouldForOne { it.tests.shouldContainExactlyInAnyOrder(firstTest, secondTest) }
        batches.shouldForOne { it.tests.shouldContainExactlyInAnyOrder(thirdTest) }
    }

    @Test
    fun `should put tests from classes with common prefix into the same batch`() {
        // given
        val numberOfBatches = 2

        val firstTest = testResult("first test", "com.example.SomeClass")
        val secondTest = testResult("second test", "com.example.SomeClassWithSuffix")
        val thirdTest = testResult("third test", "com.example.AnotherClass")
        val fourthTest = testResult("fourth test", "com.example.nested.SomeClass")
        val fifthTest = testResult("fifth test", "com.example.nested.SomeClassWithSuffix")
        val testResults = listOf(firstTest, secondTest, thirdTest, fourthTest, fifthTest)

        // when
        val batches = TestGrouper.groupIntoBatches(numberOfBatches, testResults)

        // then
        batches shouldHaveSize 2
        batches.shouldForOne {
            it.tests.shouldContainAll(firstTest, secondTest)
            it.tests.shouldNotContainAll(fourthTest, fifthTest)
        }
        batches.shouldForOne {
            it.tests.shouldContainAll(fourthTest, fifthTest)
            it.tests.shouldNotContainAll(firstTest, secondTest)
        }
    }

    @Test
    fun `should not fail when there is only one class in a package`() {
        // given
        val numberOfBatches = 2
        val testResults = listOf(
            testResult("first test", "com.example.first.FirstClass"),
            testResult("second test", "com.example.second.SecondClass"),
            testResult("third test", "com.example.third.ThirdClass")
        )

        // expect
        shouldNotThrow<Exception> {
            TestGrouper.groupIntoBatches(numberOfBatches, testResults)
        }
    }

    @Test
    fun `should support edge case`() {
        // given
        val numberOfBatches = 2
        val testResults = listOf(
            testResult("first test", "com.sksamuel.kotest.engine.spec.dsl.callbackorder.FunSpecConfigEnabledTest"),
            testResult("second test", "com.sksamuel.kotest.engine.spec.dsl.callbackorder.FunSpecNestedBeforeAfterContainerTest"),
            testResult("third test", "com.sksamuel.kotest.engine.spec.dsl.callbackorder.FunSpecNestedBeforeAfterTest"),
            testResult("fourth test", "com.sksamuel.kotest.engine.spec.dsl.callbackorder.FunSpecNestedBeforeAfterEachTest"),
            testResult("fifth test", "com.sksamuel.kotest.engine.spec.dsl.callbackorder.FunSpecNestedBeforeAfterAnyTest"),
        )

        // when
        val batches = TestGrouper.groupIntoBatches(numberOfBatches, testResults)

        // then
        batches shouldHaveSize 2
    }

    private fun testResult(name: String, classname: String) =
        TestResult(
            name = name,
            classname = classname,
            result = "successful",
            duration = 5.milliseconds
        )
}