package org.old.typecheck

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.Interval
import org.old.grammar.stellaParser

fun getLineText(tokenStream: CharStream, line: Int): String {
    val source = tokenStream.getText(Interval.of(0, tokenStream.size()))
    return source.lineSequence().drop(line).first()
}

abstract class TypeCheckException : Exception() {

    abstract fun getDescription(): String
    abstract fun getTag(): String
    override fun toString(): String {
        return """
            |tag: [${getTag()}]
            |error: ${getDescription()} 
        """.trimMargin()
    }
}


abstract class ExprException(val tree: ParserRuleContext) : TypeCheckException() {
    override fun toString(): String {
        fun Token.size(): Int {
            return this.stopIndex - this.startIndex + 1
        }

        val line = tree.start.line
        val lineText = getLineText(tree.start.inputStream, line - 1)

        val linePrefix = "$line "
        val emptyPrefix = " ".repeat(linePrefix.length)

        val stopTokenEnd = tree.stop.charPositionInLine + tree.stop.size()
        val underlineEnd = (if (tree.stop.line != line) lineText.length else stopTokenEnd).coerceAtMost(lineText.length)
        val lineStartCol = tree.start.charPositionInLine
        val underlineSize = underlineEnd - lineStartCol
        val underlinePrefix = lineText.substring(0, lineStartCol).replace(Regex("[^\\s]"), " ")
        val underline = "$emptyPrefix|$underlinePrefix${"-".repeat(underlineSize)} here"
        return """
            |${line}:${lineStartCol}: tag: [${getTag()}]
            |error: ${getDescription()} 
            |-->
            |$linePrefix|$lineText
            |$underline
        """.trimMargin()
    }
}


class MainMissing : TypeCheckException() {
    override fun getTag(): String = "ERROR_MISSING_MAIN"
    override fun getDescription(): String = "main function is not found in the program"
}

class UndefinedVariable(private val varName: stellaParser.VarContext) : ExprException(varName) {
    override fun getTag(): String = "ERROR_UNDEFINED_VARIABLE"

    override fun getDescription(): String = "variable '${varName.text}' is undefined"
}

class UnexpectedExprType(expr: ParserRuleContext, private val expectedType: Type, private val actualType: Type) :
    ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got $actualType"
}

class UnexpectedSubType(expr: ParserRuleContext, private val expectedType: Type, private val actualType: Type) :
    ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_SUBTYPE"
    override fun getDescription(): String = "unexpected subtype: expected $expectedType, but got $actualType"
}


class NotFunction(expr: ParserRuleContext, private val actualType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NOT_A_FUNCTION"
    override fun getDescription(): String = "unexpected type: expected one-argument function, but got $actualType"

}

class NotTuple(expr: ParserRuleContext, private val actualType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NOT_A_TUPLE"

    override fun getDescription(): String = "unexpected type: expected tuple, but got $actualType"
}

class NotRecord(expr: ParserRuleContext, private val actualType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NOT_A_RECORD"
    override fun getDescription(): String = "unexpected type: expected record, but got $actualType"

}

class NotList(expr: ParserRuleContext, private val actualType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NOT_A_LIST"
    override fun getDescription(): String = "unexpected type: expected list type, but got $actualType"
}

class NotRef(expr: ParserRuleContext, private val actualType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NOT_A_REFERENCE"
    override fun getDescription(): String = "unexpected type: expected reference type, but got $actualType"
}

class UnexpectedLambda(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_LAMBDA"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got lambda type"
}

class UnexpectedReference(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_REFERENCE"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got reference type"
}



class UnexpectedParamType(expr: ParserRuleContext, private val expectedType: Type, private val actualType: Type) :
    ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_TYPE_FOR_PARAMETER"
    override fun getDescription(): String = "unexpected parameter type: expected $expectedType, but got $actualType"
}

class UnexpectedTuple(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_TUPLE"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got tuple"
}



class UnexpectedRecord(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_RECORD"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got record"

}

class UnexpectedList(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_LIST"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got list"
}

class UnexpectedMemoryAddress(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_MEMORY_ADDRESS"
    override fun getDescription(): String = "unexpected type: expected $expectedType, but got memory address"
}

class MissingRecordFields(
    expr: ParserRuleContext, private val record: RecordType, private val missingFields: Iterable<String>
) : ExprException(expr) {
    override fun getTag(): String = "ERROR_MISSING_RECORD_FIELDS"
    override fun getDescription(): String {
        return "missing fields (${missingFields.joinToString(", ") { "'$it'" }}) for record $record"
    }
}

class UnexpectedRecordFields(
    expr: ParserRuleContext, private val record: RecordType, private val unexpectedFields: Iterable<String>
) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_RECORD_FIELDS"
    override fun getDescription(): String {
        return "unexpected fields (${unexpectedFields.joinToString(", ") { "'$it'" }}) for record $record"
    }
}

class UnexpectedFieldAccess(
    expr: ParserRuleContext, private val actualType: RecordType, private val accessedField: String
) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_FIELD_ACCESS"
    override fun getDescription(): String =
        "unexpected field access: there is no field '$accessedField' in record $actualType"

}

class TupleIndexOOB(expr: ParserRuleContext, private val actualType: TupleType, private val accessedIndex: Int) :
    ExprException(expr) {
    override fun getTag(): String = "ERROR_TUPLE_INDEX_OUT_OF_BOUNDS"
    override fun getDescription(): String =
        "tuple index out of bounds: there is no $accessedIndex'th field in tuple $actualType"

}

class UnexpectedTupleLength(expr: ParserRuleContext, private val actualLength: Int, private val expectedLength: Int) :
    ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_TUPLE_LENGTH"
    override fun getDescription(): String = "unexpected tuple length: expected $expectedLength but got $actualLength"

}

class AmbiguousList(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_LIST_TYPE"
    override fun getDescription(): String = "ambiguous list"
}

class AmbiguousRef(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_REFERENCE_TYPE"
    override fun getDescription(): String = "ambiguous reference"
}

class AmbiguousPanic(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_PANIC_TYPE"
    override fun getDescription(): String = "ambiguous panic"
}

class AmbiguousThrow(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_THROW_TYPE"
    override fun getDescription(): String = "ambiguous throw"
}


class AmbiguousSumType(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_SUM_TYPE"
    override fun getDescription(): String = "ambiguous sum type"
}

class AmbiguousVariantType(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_VARIANT_TYPE"
    override fun getDescription(): String = "ambiguous variant type"
}

class AmbiguousPatternType(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_AMBIGUOUS_PATTERN_TYPE"
    override fun getDescription(): String = "can not infer type of pattern"
}


class UnexpectedVariant(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr){
    override fun getTag(): String = "ERROR_UNEXPECTED_VARIANT"
    override fun getDescription(): String = "unexpected variant: expected $expectedType but got variant"
}

class UnexpectedVariantLabel(expr: ParserRuleContext, private val expectedType: Type, private val actualLabel: String) : ExprException(expr){
    override fun getTag(): String = "ERROR_UNEXPECTED_VARIANT_LABEL"
    override fun getDescription(): String = "unexpected variant label: expected type $expectedType not contains label `$actualLabel`"
}

class UnexpectedInjection(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_INJECTION"
    override fun getDescription(): String = "unexpected injection: expected $expectedType but got injection"
}

class IllegalEmptyMatching(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_ILLEGAL_EMPTY_MATCHING"
    override fun getDescription(): String = "Illegal empty matching"
}

class NonExhaustiveMatchPatterns(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NONEXHAUSTIVE_MATCH_PATTERNS"
    override fun getDescription(): String = "Non exhaustive match patterns"
}

class NonExhaustiveLetPatterns(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_NONEXHAUSTIVE_LET_PATTERNS"
    override fun getDescription(): String = "Non exhaustive let patterns"
}

class UnexpectedPatternForType(expr: ParserRuleContext, private val expectedType: Type) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_PATTERN_FOR_TYPE"
    override fun getDescription(): String = "Unexpected pattern for type $expectedType"
}

class IncorrectArityOfMain(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_INCORRECT_ARITY_OF_MAIN"
    override fun getDescription(): String = "Arity of main should be 1"
}

class IncorrectNumberOfArguments(expr: ParserRuleContext, private val expectedNumber: Int, private val actualNumber: Int) : ExprException(expr) {
    override fun getTag(): String = "ERROR_INCORRECT_NUMBER_OF_ARGUMENTS"
    override fun getDescription(): String = "Expected $expectedNumber of arguments, but got $actualNumber"
}

class UnexpectedNumberOfLambdaParameters(expr: ParserRuleContext, private val expectedNumber: Int, private val actualNumber: Int) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA"
    override fun getDescription(): String = "Expected $expectedNumber of parameters in lambda, but got $actualNumber"
}




class MissingDataForLabel(expr: ParserRuleContext, private val variantType: VariantType, private val expectedLabelType: Type, private val label: String) : ExprException(expr) {
    override fun getTag(): String = "ERROR_MISSING_DATA_FOR_LABEL"
    override fun getDescription(): String = "Label '$label' in variant $variantType expect data of type $expectedLabelType, but data missing"
}

class UnexpectedDataForNullaryLabel(expr: ParserRuleContext, private val variantType: VariantType, private val label: String) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL"
    override fun getDescription(): String = "Label '$label' in variant $variantType is nullary, but unexpected data given"
}

class UnexpectedNonNullaryVariantPattern(expr: ParserRuleContext, private val variantType: VariantType, private val label: String) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN"
    override fun getDescription(): String = "Label '$label' in record $variantType is nullary, but unexpected data given in pattern"
}

class UnexpectedNullaryVariantPattern(expr: ParserRuleContext, private val variantType: VariantType, private val label: String) : ExprException(expr) {
    override fun getTag(): String = "ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN"
    override fun getDescription(): String = "Label '$label' in record $variantType has type ${variantType.variantsTypes[label]!!}, but its pattern missing"
}

class ExceptionTypeNotDeclared(expr: ParserRuleContext) : ExprException(expr) {
    override fun getTag(): String = "ERROR_EXCEPTION_TYPE_NOT_DECLARED"
    override fun getDescription(): String = "Exception type not declared"
}






