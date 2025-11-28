import buildtypes.JvmTests
import jetbrains.buildServer.configs.kotlin.BuildSteps
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
import steps.packGradleCaches
import steps.unpackGradleCaches
import steps.unpackTestResults
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
