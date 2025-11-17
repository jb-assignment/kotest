import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.runInDocker
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2025.07"

project {
    sequentialChain {
        buildType(JvmTests)
    }
}

fun Project.sequentialChain(block: CompoundStage.() -> Unit) {
    val buildTypes = sequential(block).buildTypes()
    buildTypes.forEach(::buildType)
}

object JvmTests : BuildType() {
    init {
        name = "JVM tests"
        id("jvm_tests")

        vcs {
            root(DslContext.settingsRoot)
        }

        steps {
            gradle {
                name = "Run all tests"
                tasks = "check"
                gradleParams = "-PjvmOnly=true"
            }
        }

        features {
           runInDocker {
              dockerImage = "eclipse-temurin:21-jdk"
           }
        }

        triggers {
            vcs {  }
        }
    }
}
