package buildtypes

import com.google.common.collect.Collections2
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
        Collections2.permutations(listOf(1, 2, 3, 4, 5)).forEach { println(it) }

        name = "JVM tests"
        artifactRules = """
            +:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip
            +:gradle-caches.z*
        """.trimIndent()

        dependencies {
            artifacts(JvmTests) {
                buildRule = lastFinished()

                artifactRules = """
                    ?:test-results*.zip => test-results
                    ?:gradle-caches.z* => %env.HOME%/.gradle/caches
                """.trimIndent()
            }
        }

        val numberOfBatches = 5

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