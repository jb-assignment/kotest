val groupTestsIntoBatches by tasks.registering(GroupTestsIntoBatches::class) {
    numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
    testResultsDir = projectDir.resolve("test-results") // TODO build/test-results
    batchesOutputDir = layout.buildDirectory.dir("test-batches")
}

val prepareExcludeTestPatterns by tasks.registering(PrepareExcludeTestPatterns::class) {
    batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
    batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
    excludePatternsFile = layout.buildDirectory.file("testExcludePatterns.txt")
}

allprojects {
    tasks.withType<Test>().configureEach {
        inputs.files(prepareExcludeTestPatterns.flatMap { it.excludePatternsFile })
        outputs.cacheIf { false }
        outputs.upToDateWhen { false }

        doFirst {
            val excludePatternsFile = inputs.files.filter { it.name.endsWith(".txt") }.singleFile
            val excludePatterns = excludePatternsFile.readLines()
            excludePatterns.forEach { pattern -> filter.excludeTestsMatching(pattern) }
        }
    }
}
