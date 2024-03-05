package typecheck

import org.antlr.v4.runtime.tree.ParseTree
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.old.getParser
import org.old.typecheck.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.fail


@Suppress("TestFunctionName")
class TypeCheckerTest {
    private fun getResource(resourceFolder: String): Path {
        return Path.of(TypeCheckerVisitor::class.java.getResource(resourceFolder)!!.toURI())
    }

    private inline fun <reified T : TypeCheckException> runBadTest(program: ParseTree, testCase: String, tag: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {
            if (e !is T) {
                fail(
                    "Testcase '$testCase' throws unexpected exception! \n" +
                            "Expected '${T::class.simpleName}' but got '${e.javaClass.simpleName}'!", e
                )
            }
            println(e)
            if (!e.toString().contains(tag)) {
                fail("Testcase '$testCase' throws expected exception, but tag $tag not present in output!", e)
            }
            return
        }
        fail("Testcase '$testCase' not throws ${T::class.simpleName}!")
    }

    private fun runOkTest(program: ParseTree, testCase: String) {
        try {
            checkTypes(program)
        } catch (e: Throwable) {
            fail("'$testCase' throws unexpected exception '${e.javaClass.simpleName}': $e'!", e)
        }
    }

    private fun getTests(resourceFolder: String, run: (ParseTree, String) -> Unit): Collection<DynamicTest> {
        return Files.list(getResource(resourceFolder)).map { file ->
            DynamicTest.dynamicTest(file.fileName.toString()) {
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
    fun dynamicTestsFromStream(): Collection<DynamicContainer> {
        return mutableListOf("A", "B", "C")
            .map { input: String? ->
                dynamicContainer(
                    "Container $input", listOf(
                        dynamicTest("not null") { assertTrue(input != null) },
                        dynamicTest("not empty") { assertTrue(!input!!.isEmpty()) }
                    ))
            }
    }
    private inline fun <reified T : TypeCheckException> getBadTests(errorTag: String): Collection<DynamicTest> {
        return getTests("/stella-tests/bad/$errorTag") { program, testCase ->
            runBadTest<T>(program, testCase, errorTag)
        }
    }

    @TestFactory
    fun OkTests(): Collection<DynamicTest> {
        return getTests("/stella-tests/ok", ::runOkTest)
    }
    @TestFactory
    fun ERROR_AMBIGUOUS_LIST(): Collection<DynamicTest> {
        return getBadTests<AmbiguousList>("ERROR_AMBIGUOUS_LIST")
    }

    @TestFactory
    fun ERROR_AMBIGUOUS_SUM_TYPE(): Collection<DynamicTest> {
        return getBadTests<AmbiguousSumType>("ERROR_AMBIGUOUS_SUM_TYPE")
    }

    @TestFactory
    fun ERROR_AMBIGUOUS_VARIANT_TYPE(): Collection<DynamicTest> {
        return getBadTests<AmbiguousVariantType>("ERROR_AMBIGUOUS_VARIANT_TYPE")
    }

    @TestFactory
    fun ERROR_ILLEGAL_EMPTY_MATCHING(): Collection<DynamicTest> {
        return getBadTests<IllegalEmptyMatching>("ERROR_ILLEGAL_EMPTY_MATCHING")
    }

    @TestFactory
    fun ERROR_INCORRECT_ARITY_OF_MAIN(): Collection<DynamicTest> {
        return getBadTests<IncorrectArityOfMain>("ERROR_INCORRECT_ARITY_OF_MAIN")
    }

    @TestFactory
    fun ERROR_INCORRECT_NUMBER_OF_ARGUMENTS(): Collection<DynamicTest> {
        return getBadTests<IncorrectNumberOfArguments>("ERROR_INCORRECT_NUMBER_OF_ARGUMENTS")
    }

    @TestFactory
    fun ERROR_MISSING_DATA_FOR_LABEL(): Collection<DynamicTest> {
        return getBadTests<MissingDataForLabel>("ERROR_MISSING_DATA_FOR_LABEL")
    }


    @TestFactory
    fun ERROR_MISSING_MAIN(): Collection<DynamicTest> {
        return getBadTests<MainMissing>("ERROR_MISSING_MAIN")
    }

    @TestFactory
    fun ERROR_MISSING_RECORD_FIELDS(): Collection<DynamicTest> {
        return getBadTests<MissingRecordFields>("ERROR_MISSING_RECORD_FIELDS")
    }


    @TestFactory
    fun ERROR_NONEXHAUSTIVE_MATCH_PATTERNS(): Collection<DynamicTest> {
        return getBadTests<NonExhaustiveMatchPatterns>("ERROR_NONEXHAUSTIVE_MATCH_PATTERNS")
    }

    @TestFactory
    fun ERROR_NOT_A_FUNCTION(): Collection<DynamicTest> {
        return getBadTests<NotFunction>("ERROR_NOT_A_FUNCTION")
    }
    @TestFactory
    fun ERROR_NOT_A_LIST(): Collection<DynamicTest> {
        return getBadTests<NotList>("ERROR_NOT_A_LIST")
    }

    @TestFactory
    fun ERROR_NOT_A_RECORD(): Collection<DynamicTest> {
        return getBadTests<NotRecord>("ERROR_NOT_A_RECORD")
    }
    @TestFactory
    fun ERROR_NOT_A_TUPLE(): Collection<DynamicTest> {
        return getBadTests<NotTuple>("ERROR_NOT_A_TUPLE")
    }
    @TestFactory
    fun ERROR_TUPLE_INDEX_OUT_OF_BOUNDS(): Collection<DynamicTest> {
        return getBadTests<TupleIndexOOB>("ERROR_TUPLE_INDEX_OUT_OF_BOUNDS")
    }
    @TestFactory
    fun ERROR_UNDEFINED_VARIABLE(): Collection<DynamicTest> {
        return getBadTests<UndefinedVariable>("ERROR_UNDEFINED_VARIABLE")
    }
    @TestFactory
    fun ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL(): Collection<DynamicTest> {
        return getBadTests<UnexpectedDataForNullaryLabel>("ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL")
    }
    @TestFactory
    fun ERROR_UNEXPECTED_FIELD_ACCESS(): Collection<DynamicTest> {
        return getBadTests<UnexpectedFieldAccess>("ERROR_UNEXPECTED_FIELD_ACCESS")
    }
    @TestFactory
    fun ERROR_UNEXPECTED_INJECTION(): Collection<DynamicTest> {
        return getBadTests<UnexpectedInjection>("ERROR_UNEXPECTED_INJECTION")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_LAMBDA(): Collection<DynamicTest> {
        return getBadTests<UnexpectedLambda>("ERROR_UNEXPECTED_LAMBDA")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_LIST(): Collection<DynamicTest> {
        return getBadTests<UnexpectedList>("ERROR_UNEXPECTED_LIST")
    }
    @TestFactory
    fun ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN(): Collection<DynamicTest> {
        return getBadTests<UnexpectedNonNullaryVariantPattern>("ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN(): Collection<DynamicTest> {
        return getBadTests<UnexpectedNullaryVariantPattern>("ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA(): Collection<DynamicTest> {
        return getBadTests<UnexpectedNumberOfLambdaParameters>("ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_PATTERN_FOR_TYPE(): Collection<DynamicTest> {
        return getBadTests<UnexpectedPatternForType>("ERROR_UNEXPECTED_PATTERN_FOR_TYPE")
    }


    @TestFactory
    fun ERROR_UNEXPECTED_RECORD(): Collection<DynamicTest> {
        return getBadTests<UnexpectedRecord>("ERROR_UNEXPECTED_RECORD")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_RECORD_FIELDS(): Collection<DynamicTest> {
        return getBadTests<UnexpectedRecordFields>("ERROR_UNEXPECTED_RECORD_FIELDS")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_TUPLE(): Collection<DynamicTest> {
        return getBadTests<UnexpectedTuple>("ERROR_UNEXPECTED_TUPLE")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_TUPLE_LENGTH(): Collection<DynamicTest> {
        return getBadTests<UnexpectedTupleLength>("ERROR_UNEXPECTED_TUPLE_LENGTH")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION(): Collection<DynamicTest> {
        return getBadTests<UnexpectedExprType>("ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION")
    }



    @TestFactory
    fun ERROR_UNEXPECTED_TYPE_FOR_PARAMETER(): Collection<DynamicTest> {
        return getBadTests<UnexpectedParamType>("ERROR_UNEXPECTED_TYPE_FOR_PARAMETER")
    }



    @TestFactory
    fun ERROR_UNEXPECTED_VARIANT(): Collection<DynamicTest> {
        return getBadTests<UnexpectedVariant>("ERROR_UNEXPECTED_VARIANT")
    }

    @TestFactory
    fun ERROR_UNEXPECTED_VARIANT_LABEL(): Collection<DynamicTest> {
        return getBadTests<UnexpectedVariantLabel>("ERROR_UNEXPECTED_VARIANT_LABEL")
    }


}