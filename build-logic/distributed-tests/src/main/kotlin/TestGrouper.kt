import org.apache.commons.numbers.combinatorics.Combinations
import kotlin.time.Duration

internal object TestGrouper {

    fun groupIntoBatches(numberOfBatches: Int, testResults: List<TestResult>): List<TestBatch> {
        val batches: List<MutableTestBatch> = (1..numberOfBatches).map(::MutableTestBatch)

        val testClassesFromSlowest = findAtomicGroups(testResults)
            .sortedByDescending(AtomicTestGroup::totalDuration)

        for (testClass in testClassesFromSlowest) {
            val smallestBatch = batches.minBy(MutableTestBatch::totalDuration)
            smallestBatch.addTests(testClass.tests)
        }
        return batches.map(MutableTestBatch::toTestBatch)
    }

    private fun findAtomicGroups(testResults: List<TestResult>): List<AtomicTestGroup> {
        val testClasses = testResults
            .groupBy(TestResult::classname)
            .mapValues { (classname, tests) -> AtomicTestGroup(classname, tests) }
            .values
            .toList()

        val commonPrefixes = testClasses
            .groupBy(AtomicTestGroup::packageName)
            .mapNotNull { (packageName, classes) ->
                Combinations.of(classes.size, 2)
                    .map { Pair(classes[it[0]], classes[it[1]]) }
                    .map { (a, b) -> a.simpleClassName.commonPrefixWith(b.simpleClassName) }
                    .filter(String::isNotBlank)
                    .map { prefix -> "$packageName.$prefix" }
            }
            .flatten()

        val testClassesPerCommonPrefix = commonPrefixes
            .associateWith { prefix -> testClasses.filter { testClass -> testClass.fullyQualifiedClassName.startsWith(prefix) } }

        val testClassesWithoutCommonPrefix = testClasses
            .filter { testClass -> commonPrefixes.none { prefix -> testClass.fullyQualifiedClassName.startsWith(prefix) } }

        return testClasses.toList()
    }
}

private data class AtomicTestGroup(
    val fullyQualifiedClassName: String,
    val tests: List<TestResult>
) {
    val packageName = fullyQualifiedClassName.substringBeforeLast(".")
    val simpleClassName = fullyQualifiedClassName.substringAfterLast(".")
    val totalDuration = tests.totalDuration()

//    fun merge(other: AtomicTestGroup) =
//        AtomicTestGroup(
//            commonPrefix = this.commonPrefix.commonPrefixWith(other.commonPrefix),
//            tests = this.tests + other.tests
//        )
}

private class MutableTestBatch(val number: Int) {
    private val tests = mutableListOf<TestResult>()
    val totalDuration get() = tests.totalDuration()

    fun addTests(test: List<TestResult>) {
        tests.addAll(test)
    }

    fun toTestBatch() = TestBatch(number, tests, totalDuration)
}

private fun List<TestResult>.totalDuration() =
    map(TestResult::duration).fold(Duration.ZERO, Duration::plus)