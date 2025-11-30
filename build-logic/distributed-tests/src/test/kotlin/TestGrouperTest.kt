import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.collections.shouldBeEmpty
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

        val firstTest = testResult("first", "com.example.SomeClass")
        val secondTest = testResult("second", "com.example.SomeClass")
        val thirdTest = testResult("third", "com.example.AnotherClass")
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

        val firstTest = testResult("first", "com.example.SomeClass")
        val secondTest = testResult("second", "com.example.SomeClassWithSuffix")
        val thirdTest = testResult("third", "com.example.AnotherClass")
        val fourthTest = testResult("fourth", "com.example.nested.SomeClass")
        val fifthTest = testResult("fifth", "com.example.nested.SomeClassWithSuffix")
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
    fun `should normalize prefixes`() {
        // given
        val numberOfBatches = 2

        val firstTest = testResult("first", "com.example.FunSpecConfigEnabledTest")
        val secondTest = testResult("second", "com.example.FunSpecNestedBeforeAfterContainerTest")
        val thirdTest = testResult("third", "com.example.FunSpecNestedBeforeAfterTest")
        val fourthTest = testResult("fourth", "com.example.FunSpecNestedBeforeAfterEachTest")
        val fifthTest = testResult("fifth", "com.example.FunSpecNestedBeforeAfterAnyTest")
        val testResults = listOf(firstTest, secondTest, thirdTest, fourthTest, fifthTest)

        // when
        val batches = TestGrouper.groupIntoBatches(numberOfBatches, testResults)

        // then
        batches shouldHaveSize 2
        batches.shouldForOne { it.tests.shouldContainExactlyInAnyOrder(firstTest, secondTest, thirdTest, fourthTest, fifthTest) }
        batches.shouldForOne { it.tests.shouldBeEmpty() }
    }

    @Test
    fun `should not normalize prefixes from different packages`() {
        // given
        val numberOfBatches = 3

        val firstTest = testResult("first", "com.a.SomeClass")
        val secondTest = testResult("second", "com.a.SomeClassWithSuffix")
        val thirdTest = testResult("third", "com.b.SomeClass")
        val fourthTest = testResult("fourth", "com.b.SomeClassWithSuffix")
        val testResults = listOf(firstTest, secondTest, thirdTest, fourthTest)

        // when
        val batches = TestGrouper.groupIntoBatches(numberOfBatches, testResults)

        // then
        batches shouldHaveSize 3
        batches.shouldForOne { it.tests.shouldContainAll(firstTest, secondTest) }
        batches.shouldForOne { it.tests.shouldContainAll(thirdTest, fourthTest) }
    }

    @Test
    fun `should not fail when there is only one class in a package`() {
        // given
        val numberOfBatches = 2
        val testResults = listOf(
            testResult("first", "com.example.first.FirstClass"),
            testResult("second", "com.example.second.SecondClass"),
            testResult("third", "com.example.third.ThirdClass")
        )

        // expect
        shouldNotThrow<Exception> {
            TestGrouper.groupIntoBatches(numberOfBatches, testResults)
        }
    }

    @Test
    fun `should not fail if there is only one common prefix`() {
        // given
        val numberOfBatches = 2

        val firstTest = testResult("first", "com.example.FunSpecConfigEnabledTest")
        val secondTest = testResult("second", "com.example.FunSpecNestedBeforeAfterContainerTest")
        val testResults = listOf(firstTest, secondTest)

        // expect
        shouldNotThrow<Exception> {
            TestGrouper.groupIntoBatches(numberOfBatches, testResults)
        }
    }

    private fun testResult(name: String, classname: String) =
        TestResult(
            name = name,
            classname = classname,
            result = "successful",
            duration = 5.milliseconds
        )
}