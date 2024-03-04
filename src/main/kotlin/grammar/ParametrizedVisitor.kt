package org.old.grammar

import org.antlr.v4.runtime.tree.ParseTree

abstract class ParametrizedVisitor<Arg, Ret>(action: String) : UnimplementedStellaVisitor<Ret>(action) {
    var arg: Arg? = null
    fun visitWithArg(expr: ParseTree, arg: Arg): Ret {
        // because antlr not generate parametrized visitor, we need store argument in field
        val prevArg = this.arg
        this.arg = arg
        val res = expr.accept(this)
        this.arg = prevArg
        return res
    }
}