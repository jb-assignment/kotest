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
    }

    sequentialChain {
        buildType(Debug)
//        buildType(JvmCompile)
//        buildType(JvmTests)
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
        artifactRules = "gradle-caches.tar.gz"


        steps {
            gradle {
                tasks = "assemble"
            }

            script {
                name = "Pack Gradle Cache"
                scriptContent = """
                # Define output inside the Checkout Directory
                OUTPUT="gradle-caches.tar.gz"
                
                echo "Packing caches from ${'$'}HOME/.gradle/caches..."
                
                # -C changes directory to the cache root so the archive structure is clean
                # We explicitly list the wildcards here. The SHELL expands them, not TeamCity.
                # '|| true' ensures the build doesn't fail if optional folders (like transforms) are missing.
                
                tar -czf "${'$'}OUTPUT" \
                    -C "${'$'}HOME/.gradle/caches" \
                    modules-2 \
                    jars-9 \
                    transforms* \
                    */generated-gradle-jars \
                    */kotlin-dsl \
                    || true
                    
                echo "Created ${'$'}OUTPUT"
            """
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
            +:%gradle.location%/caches => gradle-caches.zip
        """.trimIndent()

//        dependencies {
//            artifacts(JvmTests) {
//                buildRule = lastSuccessful()
//
//                // +:test-results*.zip => test-results
//                artifactRules = """
//                    ?:gradle-caches-%batchNumber%.zip =>
//                """.trimIndent()
//            }
//        }
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

            gradle {
                tasks = "jvmTest"

                conditions {
                    doesNotExist("env.SKIP_BUILD")
                }
            }
        }

        triggers {
            vcs { }
        }
    }
}
