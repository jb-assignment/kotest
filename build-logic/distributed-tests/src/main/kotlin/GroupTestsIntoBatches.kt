import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@CacheableTask
abstract class GroupTestsIntoBatches : DefaultTask() {

    @Input
    val numberOfBatches = project.objects.property<Int>()

    @InputDirectory
    @PathSensitive(RELATIVE)
    val testResultsDir = project.objects.directoryProperty()

    @OutputDirectory
    val batchesOutputDir = project.objects.directoryProperty()

    @TaskAction
    fun execute() {
        val testResults = collectTestResults()
        val batches = groupIntoBatches(testResults)
        writeBatchesToOutputDir(batches)
        logSummaryMessage(testResults, batches)
    }

    private fun collectTestResults(): List<TestResult> =
        testResultsDir.get().asFile
            .walk()
            .filter { it.name.endsWith(".xml") }
            .flatMap(::parseXmlTestResults)
            .toList()

    private fun parseXmlTestResults(testResultsFile: File): List<TestResult> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResultsFile)
        val testcaseNodes = document.getElementsByTagName("testcase").toList()

        return buildList {
            for (node in testcaseNodes) {
                node as? Element ?: continue

                val childNodes = node.childNodes.toList()
                val result = when {
                    childNodes.any { it.nodeName == "failure" } -> "failed"
                    childNodes.any { it.nodeName == "skipped" } -> "skipped"
                    else -> "successful"
                }
                val testResult = TestResult(
                    classname = node.getAttribute("classname"),
                    name = node.getAttribute("name"),
                    result = result,
                    duration = node.getAttribute("time").toDouble().seconds,
                )
                add(testResult)
            }
        }
    }

    private fun groupIntoBatches(testResults: List<TestResult>): List<TestBatch> {
        val numberOfBatches = numberOfBatches.get()
        val batches: List<MutableTestBatch> = (1..numberOfBatches).map(::MutableTestBatch)

        val testClassesFromSlowest = testResults
            .groupBy(TestResult::classname)
            .mapValues { (classname, tests) -> TestClass(classname, tests) }
            .values
            .sortedByDescending(TestClass::totalDuration)

        for (testClass in testClassesFromSlowest) {
            val smallestBatch = batches.minBy(MutableTestBatch::totalDuration)
            smallestBatch.addTests(testClass.tests)
        }
        return batches.map(MutableTestBatch::toTestBatch)
    }

    private fun writeBatchesToOutputDir(batches: List<TestBatch>) {
        val outputDir = batchesOutputDir.get().asFile

        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        batches.forEach { batch ->
            val batchFile = outputDir.resolve("batch-${batch.number}.json")
            batchFile.createNewFile()
            batchFile.writeText(objectMapper.writeValueAsString(batch))
        }
    }

    private fun logSummaryMessage(allTestResults: List<TestResult>, batches: List<TestBatch>) {
        val message = buildString {
            appendLine("Found ${allTestResults.size} tests")
            appendLine("Grouped them into batches:")
            batches.forEach {
                appendLine("${it.number}. tests = ${it.tests.size}, total duration = ${it.totalDuration}")
            }
        }
        logger.lifecycle(message)
    }
}

private data class TestClass(
    val classname: String,
    val tests: List<TestResult>
) {
    val totalDuration = tests.totalDuration()
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

private fun NodeList.toList(): List<Node> =
    buildList {
        for (i in 0 until length) {
            add(item(i))
        }
    }
