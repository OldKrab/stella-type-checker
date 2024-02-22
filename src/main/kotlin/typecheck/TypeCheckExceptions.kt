package org.old.typecheck

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.old.grammar.stellaParser

abstract class TypeCheckException() : Exception() {
    override fun toString(): String {
        return getDescription()
    }

    open fun getDescription(): String = "${this::class.simpleName}"
}

abstract class ExprException(val tree: ParserRuleContext) : TypeCheckException() {
    override fun toString(): String {
        return "${tree.start.line}:${tree.start.charPositionInLine}: ${getDescription()}"
    }

}


class TodoException : TypeCheckException()
class MainMissing : TypeCheckException()
class UndefinedVariable(private val varName: stellaParser.VarContext) : ExprException(varName) {
    override fun getDescription(): String {
        return "${varName.text} undefined"
    }
}

class UnexpectedExprType(val expr: ParserRuleContext, val expectedType: Type, val actualType: Type) :
    ExprException(expr)

class NotFunctionApplication(val expr: ParserRuleContext, val actualType: Type) : ExprException(expr)
class NotTuple(val expr: ParserRuleContext, val actualType: Type) : ExprException(expr)
class NotRecord(val expr: ParserRuleContext, val actualType: Type) : ExprException(expr)
class NotList(val expr: ParserRuleContext, val actualType: Type) : ExprException(expr)
class UnexpectedLambda(val expr: ParserRuleContext, val expectedType: Type) : ExprException(expr)
class UnexpectedParamType(val expr: ParserRuleContext, val expectedType: Type) : ExprException(expr)
class UnexpectedTuple(val expr: ParserRuleContext, val expectedType: Type) : ExprException(expr)
class UnexpectedRecord(val expr: ParserRuleContext, val expectedType: Type) : ExprException(expr)
class UnexpectedList(val expr: ParserRuleContext, val expectedType: Type) : ExprException(expr) {

}

class MissingRecordFields(val expr: ParserRuleContext, val unexpectedFields: Iterable<String>) : ExprException(expr)

class UnexpectedRecordFields(val expr: ParserRuleContext, val unexpectedFields: Iterable<String>) : ExprException(expr)

class UnexpectedFieldAccess(val expr: ParserRuleContext, val actualType: RecordType, val accessedField: String) :
    ExprException(expr)

class TupleIndexOOB(val expr: ParserRuleContext, val actualSize: Int, val accessedIndex: Int) :
    ExprException(expr)

class UnexpectedTupleLength(val expr: ParserRuleContext, val actualSize: Int, val expectedLength: Int) :
    ExprException(expr)
