val batchNumber = System.getenv("BATCH_NUMBER")
val testsInBatch = batchNumber
    ?.let { File("batches/batch-$batchNumber.txt") }
    ?.takeIf(File::exists)
    ?.readLines()

if (testsInBatch != null) {
    println("Running ${testsInBatch.size} tests from batch $batchNumber")
} else {
    println("Running all tests")
}

allprojects {
    tasks.withType<Test>().configureEach {
        testsInBatch?.forEach { filter.includeTestsMatching("$it*") }
    }
}
