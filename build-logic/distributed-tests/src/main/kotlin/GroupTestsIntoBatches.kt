import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinFeature.UseJavaDurationConversion
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.nio.file.FileSystems
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.component1
import kotlin.collections.component2
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
        val testResults = collectTestResults(testResultsDir.get().asFile)
        val batches = groupIntoBatches(testResults)
        writeBatchesToOutputDir(batches)
        logSummaryMessage(testResults, batches)
    }

    private fun collectTestResults(testResultsDir: File): List<TestResult> {
        val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/TEST-*.xml")

        return testResultsDir
            // TODO optimize?
            .walk()
            .filter { pathMatcher.matches(it.toPath()) }
            .flatMap(::parseXmlTestResults)
            .toList()
    }

    private fun parseXmlTestResults(testResultsFile: File): List<TestResult> {
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
        logger.lifecycle("Found ${allTestResults.size} tests")

        val batchesString = batches.joinToString("\n") { (number, batch) ->
            "$number. tests = ${batch.size}, total duration = ${batch.map(TestResult::time).reduce(Duration::plus)}"
        }
        logger.lifecycle("Grouped them into batches:")
        logger.lifecycle(batchesString)
    }

    companion object {
        private val objectMapper = ObjectMapper()
            .registerKotlinModule { enable(UseJavaDurationConversion) }
            .registerModule(JavaTimeModule())
    }
}