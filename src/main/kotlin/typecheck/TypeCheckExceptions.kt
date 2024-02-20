package org.old.typecheck

import org.old.grammar.stellaParser

open class TypeCheckException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}


class TodoException: TypeCheckException()
class MainMissing: TypeCheckException()
class UndefinedVariable(val varName: stellaParser.VarContext): TypeCheckException() {
    override fun toString(): String {
        return "${varName.start.line}:${varName.start.charPositionInLine}: ${varName.text} undefined"
    }
}
