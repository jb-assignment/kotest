plugins {
    id("distributed-tests")
}

tasks {
    withType<Wrapper> {
        gradleVersion = "9.1.0"
    }
}