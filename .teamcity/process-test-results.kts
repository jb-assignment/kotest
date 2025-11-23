import java.io.File
import java.nio.file.FileSystems
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val testResults = collectTestResults(File("."))
val batches = groupIntoBatches(testResults)
batches.forEach { (batchNumber, tests) -> println("$batchNumber. tests: ${tests.size}, duration: ${tests.map(TestResult::time).reduce(Duration::plus) }") }

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

fun collectTestResults(testResultsDir: File): List<TestResult> {
    val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/build/test-results/{**,}TEST-*.xml")

    return testResultsDir
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

data class TestResult(
    val name: String,
    val classname: String,
    val time: Duration,
    val result: String,
)