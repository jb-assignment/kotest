import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.parallelTests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.add
import jetbrains.buildServer.configs.kotlin.version

version = "2025.07"

project {
    sequentialChain {
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

        steps {
            gradle {
                tasks = "jvmTest"
            }
        }

//        features {
//            parallelTests {
//                numberOfBatches = 10
//            }
//        }

        triggers {
            vcs { }
        }
    }
}
