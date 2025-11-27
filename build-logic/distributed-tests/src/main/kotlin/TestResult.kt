import java.io.Serializable
import kotlin.time.Duration

internal data class TestResult(
    val name: String,
    val classname: String,
    val time: Duration,
) : Serializable
