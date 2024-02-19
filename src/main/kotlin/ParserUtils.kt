package org.old

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
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

fun getParser(source: String): stellaParser {
    val lexer = stellaLexer(CharStreams.fromString(source))
    val tokens = CommonTokenStream(lexer)
    return stellaParser(tokens)
}