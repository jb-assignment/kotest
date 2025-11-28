import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

fun BuildSteps.unpackGradleCaches(customizer: ScriptBuildStep.() -> Unit = {}) {
    script {
        name = "Unpack Gradle caches"
        workingDir = "%env.HOME%/.gradle/caches"
        scriptContent = """
            zip -s 0 gradle-caches.zip --out merged-gradle-caches.zip || true
            unzip merged-gradle-caches.zip || true
        """

        customizer()
    }
}

fun BuildSteps.packGradleCaches(customizer: ScriptBuildStep.() -> Unit = {}) {
    script {
        name = "Pack Gradle caches"
        scriptContent = """
            OUTPUT_FILE="%teamcity.build.checkoutDir%/gradle-caches.zip"
            
            echo "Zipping caches from ${'$'}HOME/.gradle/caches..."
            
            cd ${'$'}HOME/.gradle/caches
            
            zip -r -q -s 250m "${'$'}OUTPUT_FILE" \
                modules* \
                jars* \
                transforms* \
                */generated-gradle-jars \
                */kotlin-dsl \
                */scripts \
                || true
        """

        customizer()
    }
}