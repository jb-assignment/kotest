@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation("org.apache.commons:commons-numbers-combinatorics:1.2")

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotest.assertions)
}

testing.suites {
    register<JvmTestSuite>("functionalTest")

    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()
    }
}