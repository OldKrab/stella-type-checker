package org.old.typecheck

import org.antlr.v4.runtime.tree.ParseTree

fun checkTypes(program: ParseTree) {
    program.accept(TypeCheckerVisitor())
}