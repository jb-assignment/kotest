package buildtypes

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.ui.add

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