import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.add
import jetbrains.buildServer.configs.kotlin.version

version = "2025.07"

project {
    params {
        param("teamcity.buildQueue.restartBuildAttempts", "0")
        param("teamcity.agent.dead.threshold.secs", "3600")
        param("teamcity.agent.inactive.threshold.secs", "3600")
    }

    sequentialChain {
        buildType(JvmTests)
    }
}

fun Project.sequentialChain(block: CompoundStage.() -> Unit) {
    sequential(block).buildTypes().forEach(::buildType)
}

abstract class BaseBuildType : BuildType() {
    init {
        vcs {
            root(DslContext.settingsRoot)
        }

        requirements {
            add {
                matches("teamcity.agent.jvm.os.family", "Linux")
            }
        }
    }
}

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
        artifactRules = """
            +:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip
            +:.gradle/configuration-cache/** => configuration-cache-%batchNumber%.zip
            +:gradle-caches.z* 
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
            vcs { }
        }
    }
}

fun BuildSteps.ifDoesNotExist(param: String, steps: BuildSteps.() -> Unit) {
    steps()

    items.forEach {
        it.conditions.add { doesNotExist(param) }
    }
}