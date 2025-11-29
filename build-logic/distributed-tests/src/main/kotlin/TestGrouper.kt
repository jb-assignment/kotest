import org.apache.commons.numbers.combinatorics.Combinations
import kotlin.time.Duration

internal object TestGrouper {

    fun groupIntoBatches(numberOfBatches: Int, testResults: List<TestResult>): List<TestBatch> {
        val batches: List<MutableTestBatch> = (1..numberOfBatches).map(::MutableTestBatch)

        val atomicGroupsFromSlowest = findAtomicGroups(testResults)
            .sortedByDescending(AtomicTestGroup::totalDuration)

        for (atomicGroup in atomicGroupsFromSlowest) {
            val smallestBatch = batches.minBy(MutableTestBatch::totalDuration)
            smallestBatch.addTests(atomicGroup.tests)
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
            .groupBy { it.commonPrefix.substringBeforeLast(".") }
            .filterValues { it.size >= 2 }
            .mapNotNull { (packageName, classes) ->
                Combinations.of(classes.size, 2)
                    .map { Pair(classes[it[0]], classes[it[1]]) }
                    .map { (a, b) -> a.commonPrefix.substringAfterLast(".").commonPrefixWith(b.commonPrefix.substringAfterLast(".")) }
                    .filter(String::isNotBlank)
                    .map { prefix -> "$packageName.$prefix" }
            }
            .flatten()
            .distinct()

        val testClassesWithCommonPrefix = commonPrefixes
            .map { prefix ->
                testClasses
                    .filter { testClass -> testClass.commonPrefix.startsWith(prefix) }
                    .reduce { a, b -> a.merge(prefix, b) }
            }

        val testClassesWithoutCommonPrefix = testClasses
            .filter { testClass -> commonPrefixes.none { prefix -> testClass.commonPrefix.startsWith(prefix) } }

        return testClassesWithCommonPrefix + testClassesWithoutCommonPrefix
    }
}

private data class AtomicTestGroup(
    val commonPrefix: String,
    val tests: List<TestResult>
) {
    val totalDuration = tests.totalDuration()

    fun merge(commonPrefix: String, other: AtomicTestGroup) =
        AtomicTestGroup(
            commonPrefix = commonPrefix,
            tests = this.tests + other.tests
        )
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