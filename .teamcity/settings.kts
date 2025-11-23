import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.RelativeId
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
//        buildType(GroupTestsIntoBatches)
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

//object GroupTestsIntoBatches : BaseBuildType() {
//    init {
//        name = "Group tests into batches"
//
//        dependencies {
//            artifacts(RelativeId("JvmTests")) {
//                buildRule = lastPinned()
//                artifactRules = "test-results*.zip => test-results"
//            }
//        }
//
//        steps {
//            script {
//                workingDir = "test-results"
//                scriptContent = "unzip -o '*.zip'"
//            }
//
//            kotlinScript {
//                // TODO output batch-1.txt, batch-2.txt, (...) files and upload them as artifacts
//                content = File("process-test-results.kts").readText()
//            }
//        }
//    }
//}

object JvmTests : BaseBuildType() {
    init {
        name = "JVM tests"
//        artifactRules = "+:**/build/test-results/**/TEST-*.xml => test-results-$batchNumber.zip"
        artifactRules = "+:something*.txt"

        steps {
            script {
                scriptContent = """
                    echo "batchNumber = %batchNumber%"
                    echo "Something %batchNumber%" > something-%batchNumber%.txt
                """.trimIndent()
            }

//            gradle {
//                tasks = "jvmTest"
//            }
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
