package typecheck

import org.antlr.v4.runtime.tree.ParseTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.old.getParser
import org.old.typecheck.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


class TypeCheckerTest {

    private fun getResource(resourceFolder: String): Path {
        val res = TypeCheckerVisitor::class.java.getResource(resourceFolder)
            ?: throw RuntimeException("No resource '$resourceFolder'")
        return Path.of(res.toURI())
    }


    private fun runBadTest(program: ParseTree, testCase: String, tag: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {
            println(e)
            if (!e.toString().contains(tag)) {
                fail("Testcase '$testCase' throws exception, but tag $tag not present in output!", e)
            }
            return
        }
        fail("Testcase '$testCase' not throws exception with tag $tag!")
    }

    private fun runOkTest(program: ParseTree, testCase: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {
            fail("'$testCase' throws unexpected exception '${e.javaClass.simpleName}': $e'!", e)
        }
    }

    private fun getTests(folder: Path, run: (ParseTree, String) -> Unit): Collection<DynamicTest> {
        return Files.list(folder).map { file ->
            dynamicTest(file.fileName.toString()) {
                val source = file.toFile().readText()
                println("Source:\n```\n$source\n```")
                val (_, errorListener, program) = getParser(source)
                errorListener.getSyntaxErrors().forEach { println(it) }
                assertEquals(0, errorListener.getSyntaxErrors().size)
                val testCase = file.fileName.toString()
                run(program, testCase)
            }
        }.toList()
    }

    @TestFactory
    fun badTests(): Collection<DynamicContainer> {
        val badTestsFolder = getResource("/stella-tests/bad")
        val dirs = badTestsFolder.listDirectoryEntries().filter { it.isDirectory() }
        return dirs.map { dir ->
            val tests = getTests(dir) { program, testCase ->
                runBadTest(program, testCase, dir.name)
            }
            dynamicContainer(dir.name, tests)
        }
    }


    @TestFactory
    fun OkTests(): Collection<DynamicTest> {
        return getTests(getResource("/stella-tests/ok"), ::runOkTest)
    }

    @Test
    fun fastTest(){
        val source = """
        language core;
        extend with #references, #sequencing;

        fn main(n : &Nat) -> Nat {
        	return n := 0; succ(0)
        }
        """.trimIndent()

        val (_, errorListener, program) = getParser(source)
        errorListener.getSyntaxErrors().forEach { println(it) }
        assertEquals(0, errorListener.getSyntaxErrors().size)
        checkTypes(program)
    }


}