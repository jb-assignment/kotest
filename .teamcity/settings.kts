import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.parallelTests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2025.07"

project {
    sequentialChain {
        buildType(AllTests)
    }
}

fun Project.sequentialChain(block: CompoundStage.() -> Unit) {
    val buildTypes = sequential(block).buildTypes()
    buildTypes.forEach(::buildType)
}

object AllTests : BuildType() {
    init {
        name = "All tests"

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            gradle {
                name = "Run all tests"
                tasks = "check"
            }
        }

        features {
            parallelTests {
                numberOfBatches = 10
            }
        }

        triggers {
            vcs {  }
        }
    }
}
