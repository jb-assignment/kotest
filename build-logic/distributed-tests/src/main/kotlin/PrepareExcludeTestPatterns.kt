import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

@CacheableTask
abstract class PrepareExcludeTestPatterns : DefaultTask() {

    @Input
    val batchNumber = project.objects.property<Int>()

    @InputDirectory
    @PathSensitive(NAME_ONLY)
    val batchesDir = project.objects.directoryProperty()

    @OutputFile
    val excludePatternsFile = project.objects.fileProperty()

    @TaskAction
    fun execute() {
        val batches = parseTestBatches()
        val testsToExclude = findTestsToExclude(batches)
        writeExcludesFile(testsToExclude)
        logSummaryMessage(batches)
    }

    private fun parseTestBatches(): List<TestBatch> =
        batchesDir.get()
            .asFile
            .listFiles()
            .map { objectMapper.readValue<TestBatch>(it) }

    private fun findTestsToExclude(batches: List<TestBatch>): List<TestResult> =
        batches
            .filterNot { it.number == batchNumber.get() }
            .flatMap(TestBatch::tests)

    private fun writeExcludesFile(testsToExclude: List<TestResult>) {
        val content = testsToExclude
            .map { "${it.classname}*" }
            .distinct()
            .joinToString("\n")
        excludePatternsFile.get().asFile.writeText(content)
    }

    private fun logSummaryMessage(batches: List<TestBatch>) {
        val currentBatch = batches.find { it.number == batchNumber.get() }
        val testCount = currentBatch?.tests?.size ?: 0
        logger.lifecycle("Running $testCount tests from batch ${batchNumber.get()} + any tests recently added")
    }
}
