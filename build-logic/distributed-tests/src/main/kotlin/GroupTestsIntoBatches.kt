import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
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
        val batches = TestGrouper.groupIntoBatches(numberOfBatches.get(), testResults)
        writeBatchesToOutputDir(batches)
        logSummaryMessage(testResults, batches)
    }

    private fun collectTestResults(): List<TestResult> =
        testResultsDir.get().asFile
            .walk()
            .filter { it.name.endsWith(".xml") }
            .flatMap(TestParser::parseTestResultsXmlFile)
            .toList()

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
            appendLine("= ${allTestResults.size}")
        }
        logger.lifecycle(message)
    }
}

