package org.old

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.Tree
import org.antlr.v4.runtime.tree.Trees
import org.old.grammar.stellaLexer
import org.old.grammar.stellaParser


fun Tree.getPrettyString(parser: stellaParser): String {
    fun inner(prefix: String, tree: Tree, isNotLast: Boolean): String = buildString {

        append(prefix)
        append(if (isNotLast) "├──" else "└──")
        append(Trees.getNodeText(tree, parser))
        append("\n")
        for (i in 0 until tree.childCount) {
            val childStr =
                inner(prefix + (if (isNotLast) "│   " else "    "), tree.getChild(i), i != tree.childCount - 1)
            append(childStr)

        }
    }
    return inner("", this, false)
}


class StellaErrorListener : BaseErrorListener() {
    private val syntaxErrors: MutableList<String> = ArrayList()

    fun getSyntaxErrors(): List<String> {
        return syntaxErrors
    }

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int, charPositionInLine: Int,
        msg: String?, e: RecognitionException?
    ) {
        syntaxErrors.add("line $line:$charPositionInLine $msg")
    }
}

fun getParser(source: String): Triple<stellaParser, StellaErrorListener, ParseTree> {
    val lexer = stellaLexer(CharStreams.fromString(source))
    val tokens = CommonTokenStream(lexer)
    val parser = stellaParser(tokens)
    val errorListener = StellaErrorListener()
    parser.removeErrorListeners()
    parser.addErrorListener(errorListener)
    return Triple(parser, errorListener, parser.start_Program())
}

