import java.io.Serializable
import kotlin.time.Duration

internal data class TestResult(
    val name: String,
    val classname: String,
    val result: String,
    val duration: Duration,
) : Serializable
