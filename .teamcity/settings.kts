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
    params {
        param("teamcity.buildQueue.restartBuildAttempts", "0")
        param("teamcity.agent.dead.threshold.secs", "3600")
        param("teamcity.agent.inactive.threshold.secs", "3600")
    }

    sequentialChain {
//        buildType(Debug)
//        buildType(JvmCompile)
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

object Debug : BaseBuildType() {
    init {
        name = "Debug"

        steps {
            gradle {
                tasks = "assemble"
            }
        }

        triggers {
            vcs { }
        }
    }
}

object JvmCompile : BaseBuildType() {
    init {
        name = "Compile all JVM"

        params {
            param("env.PUSH_TO_BUILD_CACHE", "true")
        }

        steps {
            gradle {
                tasks = "assemble"
            }
        }
    }
}

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
        artifactRules = """
            +:**/build/test-results/**/TEST-*.xml => test-results-1.zip
            +:gradle-cache-modules.zip => gradle-cache-modules-1.zip
            +:gradle-cache-jars.zip => gradle-cache-jars-1.zip
            +:gradle-cache-transforms.zip => gradle-cache-transforms-1.zip
            +:gradle-cache-generated-jars.zip => gradle-cache-generated-jars-1.zip
            +:gradle-cache-kotlin-dsl => gradle-cache-kotlin-1-dsl
            +:gradle-cache-scripts.zip => gradle-cache-scripts-1.zip
        """.trimIndent()

        dependencies {
            artifacts(JvmTests) {
                buildRule = lastSuccessful()

                // +:test-results*.zip => test-results
                artifactRules = """
                    ?:gradle-cache*.zip => %env.HOME%/.gradle/caches
                """.trimIndent()
            }
        }
//
//        features {
//            matrix {
//                param("batchNumber", (1..10).map { value(it.toString()) })
//            }
//        }
//
//        params {
//            param("env.BATCH_NUMBER", "%batchNumber%")
//        }

        steps {
//            script {
//                workingDir = "test-results"
//                scriptContent = "unzip -o '*.zip'"
//            }
//
//            script {
//                name = "Check if the build should run"
//                scriptContent = """
//                    FILE="test-results/test-results-1.zip"
//
//                    if [ "%batchNumber%" = "1" ]; then
//                        echo "Batch 1 detected: Always running (skipping file check)."
//                    elif [ -f "${'$'}FILE" ]; then
//                        echo "File '${'$'}FILE' found. Proceeding."
//                    else
//                        echo "File '${'$'}FILE' missing. Skipping build."
//
//                        echo "##teamcity[buildStatus text='Batch skipped']"
//                        echo "##teamcity[setParameter name='env.SKIP_BUILD' value='true']"
//                        exit 0
//                    fi
//                """.trimIndent()
//            }

            script {
                workingDir = "%env.HOME%/.gradle/caches"
                scriptContent = "unzip -o '*.zip'"
            }

            gradle {
                tasks = "jvmTest"

                conditions {
                    doesNotExist("env.SKIP_BUILD")
                }
            }

            script {
                name = "Pack Gradle Cache"
                scriptContent = """
                    OUTPUT_DIR="%teamcity.build.checkoutDir%"
                    
                    echo "Zipping caches from ${'$'}HOME/.gradle/caches..."
                    
                    cd ${'$'}HOME/.gradle/caches
                    
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-modules.zip" modules* || true
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-jars.zip" jars* || true
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-transforms.zip" transforms* || true
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-generated-jars.zip" */generated-gradle-jars || true
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-kotlin-dsl.zip" */kotlin-dsl || true
                    zip -r -q "${'$'}OUTPUT_DIR/gradle-cache-scripts.zip" */scripts || true
                    
                    echo "Zipped successfully:"
                    ls -l gradle-cache*.zip
                """
            }
        }

        triggers {
            vcs { }
        }
    }
}
