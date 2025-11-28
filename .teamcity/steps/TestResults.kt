package steps

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

fun BuildSteps.unpackTestResults(customizer: ScriptBuildStep.() -> Unit = {}) {
    script {
        name = "Unpack test results"
        workingDir = "test-results"
        scriptContent = """
            unzip -o '*.zip' || true
            
            FILE="test-results/test-results-1.zip"

            if [ "%batchNumber%" = "1" ]; then
                echo "Batch 1 detected: Always running (skipping file check)."
            elif [ -f "${'$'}FILE" ]; then
                echo "File '${'$'}FILE' found. Proceeding."
            else
                echo "File '${'$'}FILE' missing. Skipping build."

                echo "##teamcity[buildStatus text='Batch skipped']"
                echo "##teamcity[setParameter name='env.SKIP_BUILD' value='true']"
                exit 0
            fi
        """
    }
}