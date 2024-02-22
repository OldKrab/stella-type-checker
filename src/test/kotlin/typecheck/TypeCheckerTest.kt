package typecheck

import org.antlr.v4.runtime.tree.ParseTree
import org.junit.jupiter.api.Test
import org.old.getParser
import org.old.typecheck.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.fail

@Suppress("TestFunctionName")
class TypeCheckerTest {
    inline fun <reified T : TypeCheckException> runBadTest(program: ParseTree, testCase: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {
            if (e !is T) {
                val errMsg = "Testcase '$testCase' throws unexpected exception! \n" +
                        "Expected '${T::class.simpleName}' but got '${e.javaClass.simpleName}'!"
                fail(errMsg, e)
            }
            return
        }
        fail("Testcase '$testCase' not throws ${T::class.simpleName}!")
    }

    fun runOkTest(program: ParseTree, testCase: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {

            fail("'$testCase' throws unexpected exception '${e.javaClass.simpleName}': $e'!", e)
        }
    }

    inline fun <reified T : TypeCheckException> runBadTests(testFolder: String) {
        runTests("/stella-tests/bad/$testFolder") { program, testCase ->
            runBadTest<T>(program, testCase)
        }
    }

    fun runTests(resourceFolder: String, run: (ParseTree, String) -> Unit) {
        val testFolderPath =
            Path.of(TypeCheckerVisitor::class.java.getResource(resourceFolder)!!.toURI())
        val files = Files.list(testFolderPath)
        for (file in files) {
            val source = file.toFile().readText()
            val testCase = file.fileName.toString()
            println("Run test case $testCase...")
            val (_, errorListener, program) = getParser(source)
            errorListener.getSyntaxErrors().forEach { println(it) }
            assertEquals(0, errorListener.getSyntaxErrors().size)
            run(program, testCase)
        }
    }

    @Test
    fun OkTests() {
        runTests("/stella-tests/ok", ::runOkTest)
    }

    @Test
    fun ERROR_MISSING_MAIN() {
        runBadTests<MainMissing>("ERROR_MISSING_MAIN")
    }

    @Test
    fun ERROR_MISSING_RECORD_FIELDS() {
        runBadTests<MissingRecordFields>("ERROR_MISSING_RECORD_FIELDS")
    }

    @Test
    fun ERROR_NOT_A_FUNCTION() {
        runBadTests<NotFunctionApplication>("ERROR_NOT_A_FUNCTION")
    }

    @Test
    fun ERROR_NOT_A_RECORD() {
        runBadTests<NotRecord>("ERROR_NOT_A_RECORD")
    }

    @Test
    fun ERROR_NOT_A_TUPLE() {
        runBadTests<NotTuple>("ERROR_NOT_A_TUPLE")
    }


    @Test
    fun ERROR_NOT_A_LIST() {
        runBadTests<NotList>("ERROR_NOT_A_LIST")
    }
    @Test
    fun ERROR_TUPLE_INDEX_OUT_OF_BOUNDS() {
        runBadTests<TupleIndexOOB>("ERROR_TUPLE_INDEX_OUT_OF_BOUNDS")
    }

    @Test
    fun ERROR_UNEXPECTED_FIELD_ACCESS() {
        runBadTests<UnexpectedFieldAccess>("ERROR_UNEXPECTED_FIELD_ACCESS")
    }

    @Test
    fun ERROR_UNEXPECTED_LIST() {
        runBadTests<UnexpectedList>("ERROR_UNEXPECTED_LIST")
    }

    @Test
    fun ERROR_UNEXPECTED_LAMBDA() {
        runBadTests<UnexpectedLambda>("ERROR_UNEXPECTED_LAMBDA")
    }

    @Test
    fun ERROR_UNEXPECTED_RECORD() {
        runBadTests<UnexpectedRecord>("ERROR_UNEXPECTED_RECORD")
    }

    @Test
    fun ERROR_UNEXPECTED_RECORD_FIELDS() {
        runBadTests<UnexpectedRecordFields>("ERROR_UNEXPECTED_RECORD_FIELDS")
    }

    @Test
    fun ERROR_UNEXPECTED_TUPLE() {
        runBadTests<UnexpectedTuple>("ERROR_UNEXPECTED_TUPLE")
    }

    @Test
    fun ERROR_UNEXPECTED_TUPLE_LENGTH() {
        runBadTests<UnexpectedTupleLength>("ERROR_UNEXPECTED_TUPLE_LENGTH")
    }

    @Test
    fun ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION() {
        runBadTests<UnexpectedExprType>("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION")
    }

    @Test
    fun ERROR_UNDEFINED_VARIABLE() {
        runBadTests<UndefinedVariable>("ERROR_UNDEFINED_VARIABLE")
    }

    @Test
    fun ERROR_UNEXPECTED_TYPE_FOR_PARAMETER() {
        runBadTests<UnexpectedParamType>("ERROR_UNEXPECTED_TYPE_FOR_PARAMETER")
    }
}