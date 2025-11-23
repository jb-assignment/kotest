import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.add
import jetbrains.buildServer.configs.kotlin.version
import java.io.File

version = "2025.07"

project {
    sequentialChain {
//        buildType(JvmCompile)
        buildType(JvmTests)
        buildType(GroupTestsIntoBatches)
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

object JvmCompile : BaseBuildType() {
    init {
        name = "Compile all JVM"

        steps {
            gradle {
                tasks = "compileAllKotlinJvm"
            }
        }

        params {
            param("env.PUSH_TO_BUILD_CACHE", "true")
        }
    }
}

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
        artifactRules = "+:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip"

        params {
            param("env.BATCH_NUMBER", "%batchNumber%")
        }

        steps {
            script {
                name = "Check if the build should run"
                scriptContent = """
                    BATCH="%batchNumber%"
                    FILE="batches/batch-${'$'}BATCH.txt"
                    
                    if [ "${'$'}BATCH" = "1" ]; then
                        echo "Batch 1 detected: Always running (skipping file check)."
                    elif [ -f "${'$'}FILE" ]; then
                        echo "File '${'$'}FILE' found. Proceeding."
                    else
                        echo "File '${'$'}FILE' missing. Skipping build."
                        
                        echo "##teamcity[buildStatus text='Batch skipped']"
                        echo "##teamcity[setParameter name='env.SKIP_BUILD' value='true']"
                        exit 0
                    fi
                """.trimIndent()
            }

            gradle {
                tasks = "jvmTest"
                gradleParams = "--init-script .teamcity-init-scripts/src/main/kotlin/distributed-tests.init.gradle.kts"

                conditions {
                    doesNotExist("env.SKIP_BUILD")
                }
            }
        }

        dependencies {
            artifacts(GroupTestsIntoBatches) {
                buildRule = lastSuccessful()
                artifactRules = "?:batch*.txt => batches"
            }
        }

        features {
            matrix {
                param("batchNumber", (1..10).map { value(it.toString()) })
            }
        }

        triggers {
            vcs { }
        }
    }
}

object GroupTestsIntoBatches : BaseBuildType() {
    init {
        name = "Group tests into batches"
        artifactRules = "+:batch*.txt"

        steps {
            script {
                workingDir = "test-results"
                scriptContent = "unzip -o '*.zip'"
            }

            kotlinScript {
                content = File("process-test-results.kts").readText()
            }
        }

        dependencies {
            artifacts(JvmTests) {
                buildRule = lastSuccessful()
                artifactRules = "+:test-results*.zip => test-results"
            }
        }
    }
}
