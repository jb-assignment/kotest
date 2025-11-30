import dev.panuszewski.distributedkotest.teamcity.DistributedTests
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.project
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

    val jvmTests = DistributedTests(
        testTask = "jvmTest",
        numberOfBatches = 5,
        debugMode = true
    ) {
        vcs {
            root(DslContext.settingsRoot)
        }

        triggers {
            vcs { }
        }

        requirements {
            add {
                matches("teamcity.agent.jvm.os.family", "Linux")
            }
        }
    }

    buildType(jvmTests)
}