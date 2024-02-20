package org.old

import org.old.typecheck.TypeCheckException
import org.old.typecheck.checkTypes
import kotlin.system.exitProcess


fun main() {
    val source = System.`in`.bufferedReader().readText()
    val (parser, errorListener) = getParser(source)

    val program = parser.start_Program()
    if (errorListener.getSyntaxErrors().isNotEmpty()) {
        System.err.println("Got parse errors:")
        for (syntaxError in errorListener.getSyntaxErrors())
            System.err.println(syntaxError)
        exitProcess(1)
    }

    try {
        checkTypes(program, parser)
    } catch (e: TypeCheckException) {
        System.err.println("Got type check error:")
        println(e)
        exitProcess(2)
    }
}