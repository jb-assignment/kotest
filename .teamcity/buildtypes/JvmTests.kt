package buildtypes

import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import steps.packGradleCaches
import steps.unpackGradleCaches
import steps.unpackTestResults
import util.ifDoesNotExist

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
        artifactRules = """
            +:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip
            +:.gradle/configuration-cache/** => configuration-cache-%batchNumber%.zip
            +:gradle-caches.z*
            +:build/reports/configuration-cache/** => config-cache-report.zip
        """.trimIndent()

        dependencies {
            artifacts(JvmTests) {
                buildRule = lastFinished()

                artifactRules = """
                    ?:test-results*.zip => test-results
                    ?:configuration-cache-%batchNumber%.zip!/** => .gradle/configuration-cache 
                    ?:gradle-caches.z* => %env.HOME%/.gradle/caches
                """.trimIndent()
            }
        }

        val numberOfBatches = 1

        features {
            matrix {
                param("batchNumber", (1..numberOfBatches).map { value(it.toString()) })
            }
        }

        params {
            param("env.BATCH_NUMBER", "%batchNumber%")
            param("env.NUMBER_OF_BATCHES", "$numberOfBatches")
        }

        steps {
            script {
                scriptContent = "ls /.gradle/configuration-cache"
            }

            unpackTestResults()

            ifDoesNotExist("env.SKIP_BUILD") {
                unpackGradleCaches()

                gradle {
                    tasks = "jvmTest"
                }

                script {
                    name = "Clear previous test results"
                    scriptContent = "rm -rf test-results"
                }

                packGradleCaches()
            }
        }

        triggers {
            vcs {  }
        }
    }
}