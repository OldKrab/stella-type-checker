package org.old.grammar

import org.antlr.v4.runtime.tree.ParseTree

abstract class ParametrizedVisitor<Arg : Any, Ret>(action: String) : UnimplementedStellaVisitor<Ret>(action) {
    lateinit var arg: Arg
    fun visitWithArg(expr: ParseTree, arg: Arg): Ret {
        // because antlr not generate parametrized visitor, we need store argument in field
        if (this::arg.isInitialized) {
            val prevArg = this.arg
            this.arg = arg
            val res = expr.accept(this)
            this.arg = prevArg
            return res
        } else {
            this.arg = arg
            return expr.accept(this)
        }
    }
}