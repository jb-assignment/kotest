import buildtypes.JvmTests
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.version
import util.sequentialChain

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
