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
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
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
        val testResults = collectTestResults(testResultsDir.get().asFile)
        val batches = groupIntoBatches(testResults)
        writeBatchesToOutputDir(batches)
        logSummaryMessage(testResults, batches)
    }

    private fun collectTestResults(testResultsDir: File): List<TestResult> =
        testResultsDir
            .walk()
            .filter { it.name.endsWith(".xml") }
            .flatMap(::parseXmlTestResults)
            .toList()

    private fun parseXmlTestResults(testResultsFile: File): List<TestResult> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResultsFile)
        val testcaseNodes = document.getElementsByTagName("testcase")

        return buildList {
            for (i in 0 until testcaseNodes.length) {
                val node = testcaseNodes.item(i)
                if (node is Element) {
                    val testResult = TestResult(
                        classname = node.getAttribute("classname"),
                        name = node.getAttribute("name"),
                        time = node.getAttribute("time").toDouble().seconds,
                    )
                    add(testResult)
                }
            }
        }
    }

    private fun groupIntoBatches(testResults: List<TestResult>): List<TestBatch> {
        val numberOfBatches = numberOfBatches.get()
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

        return batches.entries.map { (number, tests) -> TestBatch(number, tests) }
    }

    private fun writeBatchesToOutputDir(batches: List<TestBatch>) {
        val outputDir = batchesOutputDir.get().asFile
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
