package org.old

import org.old.typecheck.TypeCheckException
import org.old.typecheck.checkTypes
import kotlin.system.exitProcess


fun main() {
    val source = System.`in`.bufferedReader().readText()
    val (_, errorListener, program) = getParser(source)


    if (errorListener.getSyntaxErrors().isNotEmpty()) {
        System.err.println("Got parse errors:")
        for (syntaxError in errorListener.getSyntaxErrors())
            System.err.println(syntaxError)
        exitProcess(1)
    }

    try {
        checkTypes(program)
    } catch (e: TypeCheckException) {
        println(e)
        exitProcess(2)
    }
}