package org.old.typecheck

import org.antlr.v4.runtime.tree.ParseTree
import org.old.grammar.stellaParser
import org.old.grammar.stellaParserBaseVisitor

class TypeCheckerVisitor : stellaParserBaseVisitor<Unit>() {
    val typeContext = TypeContext()
    val typeInfererVisitor = TypeInfererVisitor()
    val variablesDeclarator = VariablesDeclarator()

    companion object {
        const val NAT_ADD_FUN = "Nat::add"
    }

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        val isMainContains = ctx.decls.filterIsInstance<stellaParser.DeclFunContext>().any { it.name.text == "main" }
        if (!isMainContains)
            throw MainMissing()
        ctx.decls.forEach { it.accept(this) }
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        variablesDeclarator.declare(ctx)
        ctx.paramDecls.forEach { variablesDeclarator.declare(it) }
        ctx.localDecls.forEach { variablesDeclarator.declare(it) }

        val declReturnType = ctx.returnType.accept(TypeConverter)
        val actualReturnType = ctx.returnExpr.accept(typeInfererVisitor)
        if (!isAssignable(actualReturnType, declReturnType)) TODO("$actualReturnType, $declReturnType")

        ctx.paramDecls.forEach { variablesDeclarator.forget(it) }
        ctx.localDecls.forEach { variablesDeclarator.forget(it) }
    }

    object TypeConverter : stellaParserBaseVisitor<Type>() {
        override fun defaultResult(): Type {
            TODO()
        }

        override fun visitTypeNat(ctx: stellaParser.TypeNatContext?): Type = NatType

        override fun visitTypeUnit(ctx: stellaParser.TypeUnitContext?): Type = UnitType

        override fun visitTypeBool(ctx: stellaParser.TypeBoolContext?): Type = BoolType


        override fun visitTypeParens(ctx: stellaParser.TypeParensContext): Type {
            return ctx.type_.accept(this)
        }

        override fun visitTypeFun(ctx: stellaParser.TypeFunContext): Type {
            val paramTypes = ctx.paramTypes.map { it.accept(this) }
            val retType = ctx.returnType.accept(this)
            return FunType(paramTypes, retType)
        }

        override fun visitTypeTuple(ctx: stellaParser.TypeTupleContext): Type {
            val fieldsTypes = ctx.types.map { it.accept(this) }
            return TupleType(fieldsTypes)
        }

        override fun visitTypeRecord(ctx: stellaParser.TypeRecordContext): Type {
            val fieldsTypes = ctx.fieldTypes.associate { Pair(it.label.text, it.type_.accept(TypeConverter)) }
            return RecordType(fieldsTypes)
        }


    }

    inner class VariablesDeclarator : stellaParserBaseVisitor<Unit>() {
        var isDeclaring = true
        fun declare(ctx: ParseTree) {
            isDeclaring = true
            ctx.accept(this)
        }

        fun forget(ctx: ParseTree) {
            isDeclaring = false
            ctx.accept(this)
        }

        override fun defaultResult() {
            TODO()
        }

        override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
            val funName = ctx.name.text

            if (isDeclaring) {
                val paramsTypes = ctx.paramDecls.map { it.paramType.accept(TypeConverter) }
                val retType = ctx.returnType.accept(TypeConverter)
                val funType = FunType(paramsTypes, retType)
                typeContext.addVariable(funName, funType)
            } else
                typeContext.removeVariable(funName)
        }

        override fun visitParamDecl(ctx: stellaParser.ParamDeclContext) {
            val varName = ctx.name.text
            if (isDeclaring)
                typeContext.addVariable(varName, ctx.paramType.accept(TypeConverter))
            else
                typeContext.removeVariable(varName)
        }

        override fun visitPatternBinding(ctx: stellaParser.PatternBindingContext) {
            val pat = ctx.pat
            val varName = if (pat is stellaParser.PatternVarContext) pat.name.text else TODO()
            if (isDeclaring)
                typeContext.addVariable(varName, ctx.rhs.accept(typeInfererVisitor))
            else
                typeContext.removeVariable(varName)
        }
    }

    inner class TypeInfererVisitor : stellaParserBaseVisitor<Type>() {

        override fun defaultResult(): Type {
            TODO()
        }

        override fun visitVar(ctx: stellaParser.VarContext): Type {
            val varName = ctx.name.text
            if (varName == NAT_ADD_FUN)
                return FunType(listOf(NatType), FunType(listOf(NatType), NatType))
            val varType = typeContext.getVarType(varName) ?: throw UndefinedVariable(ctx)
            return varType
        }

        override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext): Type {
            return ctx.expr_.accept(this)
        }

        override fun visitTuple(ctx: stellaParser.TupleContext): Type {
            val paramsTypes = ctx.exprs.map { it.accept(this) }
            return TupleType(paramsTypes)
        }

        override fun visitRecord(ctx: stellaParser.RecordContext): Type {
            val paramsTypes = ctx.bindings.associate { Pair(it.name.text, it.rhs.accept(this)) }
            return RecordType(paramsTypes)
        }

        override fun visitTypeAsc(ctx: stellaParser.TypeAscContext): Type {
            val exprType = ctx.expr_.accept(this)
            val asType = ctx.type_.accept(TypeConverter)
            if (!isAssignable(exprType, asType)) TODO()
            return asType
        }

        override fun visitDotTuple(ctx: stellaParser.DotTupleContext): Type {
            val tuple = ctx.expr_.accept(this)
            if (tuple !is TupleType) TODO()
            val idx = ctx.index.text.toInt() - 1
            if (idx < 0 || idx >= tuple.fieldsTypes.size) TODO()
            return tuple.fieldsTypes[idx]
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext): Type {
            val record = ctx.expr_.accept(this)
            if (record !is RecordType) TODO()
            val label = ctx.label.text
            val type = record.fieldsTypes[label] ?: TODO()
            return type
        }

        override fun visitConstUnit(ctx: stellaParser.ConstUnitContext?): Type = UnitType

        override fun visitEqual(ctx: stellaParser.EqualContext): Type {
            val lhsType = ctx.left.accept(this)
            val rhsType = ctx.right.accept(this)
            if (!isAssignable(lhsType, rhsType)) TODO()
            return BoolType
        }

        override fun visitSucc(ctx: stellaParser.SuccContext): Type {
            val argType = ctx.n.accept(this)
            if (!isAssignable(argType, NatType)) TODO()
            return NatType
        }

        override fun visitConstInt(ctx: stellaParser.ConstIntContext): Type = NatType
        override fun visitConstFalse(ctx: stellaParser.ConstFalseContext): Type = BoolType
        override fun visitConstTrue(ctx: stellaParser.ConstTrueContext): Type = BoolType

        override fun visitAbstraction(ctx: stellaParser.AbstractionContext): Type {
            ctx.paramDecls.forEach { variablesDeclarator.declare(it) }
            val paramsTypes = ctx.paramDecls.map { it.accept(this) }
            val retType = ctx.returnExpr.accept(this)
            ctx.paramDecls.forEach { variablesDeclarator.forget(it) }
            return FunType(paramsTypes, retType)
        }

        override fun visitLet(ctx: stellaParser.LetContext): Type {
            ctx.patternBindings.forEach { variablesDeclarator.declare(it) }
            val type = ctx.body.accept(this)
            ctx.patternBindings.forEach { variablesDeclarator.forget(it) }
            return type
        }

        override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): Type {
            return ctx.paramType.accept(TypeConverter)
        }

        override fun visitNatRec(ctx: stellaParser.NatRecContext): Type {
            val nType = ctx.n.accept(this)
            val initialType = ctx.initial.accept(this)
            val stepType = ctx.step.accept(this)
            if (!isAssignable(nType, NatType)) TODO()
            if (!isAssignable(stepType, FunType(listOf(NatType), FunType(listOf(initialType), initialType)))) TODO()
            return initialType
        }

        override fun visitApplication(ctx: stellaParser.ApplicationContext): Type {
            val funType = ctx.`fun`.accept(this)
            if (funType !is FunType) TODO()
            val argsTypes = ctx.args.map { it.accept(this) }
            argsTypes.zip(funType.paramsTypes)
                .forEach { (argType, paramType) -> if (!isAssignable(argType, paramType)) TODO() }
            return funType.retType
        }

        override fun visitIf(ctx: stellaParser.IfContext): Type {
            val condType = ctx.condition.accept(this)
            val thenType = ctx.thenExpr.accept(this)
            val elseType = ctx.elseExpr.accept(this)
            if (!isAssignable(condType, BoolType)) TODO()
            if (!isAssignable(thenType, elseType)) TODO()
            return thenType
        }

    }
}