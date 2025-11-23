import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.file.FileSystems
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO move it to task? or build service? or something?
val batchNumber = System.getenv("BATCH_NUMBER")?.toInt()
val testResults = collectTestResults(rootProject.projectDir.resolve("test-results"))
val batches = groupIntoBatches(testResults)
val myBatch = batchNumber?.let { batches[it] }

if (myBatch != null) {
    logger.lifecycle("Found ${testResults.size} tests")

    val batchesString = batches.entries.joinToString(separator = "\n") { (number, batch) ->
        "$number. tests = ${batch.size}, total duration = ${batch.map(TestResult::time).reduce(Duration::plus)}"
    }
    logger.lifecycle("Grouped them into batches:")
    logger.lifecycle(batchesString)
    logger.lifecycle("Running ${myBatch.size} tests from batch $batchNumber")
} else {
    logger.lifecycle("Running all tests")
}

allprojects {
    tasks.withType<Test>().configureEach {
        myBatch?.forEach { filter.includeTestsMatching("${it.classname}*") }
    }
}

fun collectTestResults(testResultsDir: File): List<TestResult> {
    val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/TEST-*.xml")

    return testResultsDir
        // TODO optimize?
        .walk()
        .filter { pathMatcher.matches(it.toPath()) }
        .flatMap(::parseXmlTestResults)
        .toList()
}

fun parseXmlTestResults(testResultsFile: File): List<TestResult> {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResultsFile)

    val testcaseNodes = document.getElementsByTagName("testcase")
    val results = mutableListOf<TestResult>()

    for (i in 0 until testcaseNodes.length) {
        val node = testcaseNodes.item(i)
        if (node.nodeType == Node.ELEMENT_NODE) {
            val element = node as Element
            val classname = element.getAttribute("classname")
            val name = element.getAttribute("name")
            val time = element.getAttribute("time").toDouble()

            val hasFailure = element.getElementsByTagName("failure").length > 0
            val hasSkipped = element.getElementsByTagName("skipped").length > 0

            val result = when {
                hasFailure -> "failed"
                hasSkipped -> "skipped"
                else -> "successful"
            }

            results.add(
                TestResult(
                    classname = classname,
                    name = name,
                    time = time.seconds,
                    result = result
                )
            )
        }
    }
    return results
}

fun groupIntoBatches(testResults: List<TestResult>): Map<Int, List<TestResult>> {
    val numberOfBatches = 10
    val batches = mutableMapOf<Int, MutableList<TestResult>>()

    for (i in 1..numberOfBatches) {
        batches[i] = mutableListOf()
    }

    val testsByClass = testResults
        .groupBy { it.classname }
        .map { (_, tests) -> tests }
        .sortedByDescending { classTests -> classTests.sumOf { it.time.inWholeMilliseconds } }

    testsByClass.forEach { classTests ->
        val smallestBatch = batches.minBy { (_, tests) -> tests.sumOf { it.time.inWholeMilliseconds } }
        smallestBatch.value.addAll(classTests)
    }

    return batches
}

data class TestResult(
    val name: String,
    val classname: String,
    val time: Duration,
    val result: String,
)