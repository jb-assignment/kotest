package util

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.CompoundStage
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.sequential
import jetbrains.buildServer.configs.kotlin.ui.add

fun Project.sequentialChain(block: CompoundStage.() -> Unit) {
    sequential(block).buildTypes().forEach(::buildType)
}

fun BuildSteps.ifDoesNotExist(param: String, steps: BuildSteps.() -> Unit) {
    steps()

    items.forEach {
        it.conditions.add { doesNotExist(param) }
    }
}