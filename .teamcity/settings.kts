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
                tasks = "compileAllKotlinJvm"
            }
        }
    }
}

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
        artifactRules = """
            +:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip
            +:gradle-caches.z* 
        """.trimIndent()

        dependencies {
            artifacts(JvmTests) {
                buildRule = lastFinished()

                artifactRules = """
                    +:test-results*.zip => test-results
                    ?:gradle-caches.z* => %env.HOME%/.gradle/caches
                """.trimIndent()
            }
        }

        features {
            matrix {
                param("batchNumber", (1..1).map { value(it.toString()) })
            }
        }

        params {
            param("env.BATCH_NUMBER", "%batchNumber%")
            param("env.NUMBER_OF_BATCHES", "1")
        }

        steps {
            script {
                workingDir = "test-results"
                scriptContent = "unzip -o '*.zip'"
            }

            script {
                name = "Check if the build should run"
                scriptContent = """
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
                """.trimIndent()
            }

            script {
                workingDir = "%env.HOME%/.gradle/caches"
                scriptContent = """
                    zip -s 0 gradle-caches.zip --out merged-gradle-caches.zip 
                    unzip merged-gradle-caches.zip
                """

                conditions { doesNotExist("env.SKIP_BUILD") }
            }

            gradle {
                tasks = "jvmTest"

                conditions { doesNotExist("env.SKIP_BUILD") }
            }

            script {
                name = "Clear previous test results"
                scriptContent = "rm -rf test-results"
            }

            script {
                name = "Pack Gradle Cache"
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

                conditions { doesNotExist("env.SKIP_BUILD") }
            }
        }

        triggers {
            vcs { }
        }
    }
}
