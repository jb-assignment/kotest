import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration.Companion.seconds

internal object TestParser {

    fun parseTestResultsXmlFile(testResultsFile: File): List<TestResult> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResultsFile)
        val testcaseNodes = document.getElementsByTagName("testcase").toList()

        return buildList {
            for (node in testcaseNodes) {
                node as? Element ?: continue

                val childNodes = node.childNodes.toList()
                val result = when {
                    childNodes.any { it.nodeName == "failure" } -> "failed"
                    childNodes.any { it.nodeName == "skipped" } -> "skipped"
                    else -> "successful"
                }
                val testResult = TestResult(
                    classname = node.getAttribute("classname"),
                    name = node.getAttribute("name"),
                    result = result,
                    duration = node.getAttribute("time").toDouble().seconds,
                )
                add(testResult)
            }
        }
    }
}

private fun NodeList.toList(): List<Node> =
    buildList {
        for (i in 0 until length) {
            add(item(i))
        }
    }