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

    override fun visitStart_Program(ctx: stellaParser.Start_ProgramContext) = ctx.program().accept(this)

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        val isMainContains = ctx.decls.filterIsInstance<stellaParser.DeclFunContext>().any { it.name.text == "main" }
        if (!isMainContains)
            throw MainMissing()
        ctx.decls.forEach { it.accept(this) }
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        variablesDeclarator.declare(ctx)
        val vars = variablesDeclarator.declareAll(ctx.paramDecls) + variablesDeclarator.declareAll(ctx.localDecls)

        val declReturnType = ctx.returnType.accept(TypeConverter)
        typeInfererVisitor.expectType(ctx.returnExpr, declReturnType)

        typeContext.removeAllVariables(vars)
    }

    override fun defaultResult() {
        TODO()
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

        override fun visitTypeList(ctx: stellaParser.TypeListContext): Type {
            val elemType = ctx.type_.accept(TypeConverter)
            return ListType(elemType)
        }


    }

    inner class VariablesDeclarator : stellaParserBaseVisitor<String>() {
        fun declare(ctx: ParseTree): String {
            return ctx.accept(this)
        }

        fun declareAll(trees: Iterable<ParseTree>): List<String> {
            return trees.map { it.accept(this) }
        }

        override fun defaultResult(): String {
            TODO()
        }

        override fun visitDeclFun(ctx: stellaParser.DeclFunContext): String {
            val funName = ctx.name.text

            val paramsTypes = ctx.paramDecls.map { it.paramType.accept(TypeConverter) }
            val retType = ctx.returnType.accept(TypeConverter)
            val funType = FunType(paramsTypes, retType)
            typeContext.addVariable(funName, funType)
            return funName
        }

        override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): String {
            val varName = ctx.name.text
            typeContext.addVariable(varName, ctx.paramType.accept(TypeConverter))
            return varName
        }

        override fun visitPatternBinding(ctx: stellaParser.PatternBindingContext): String {
            val pat = ctx.pat
            val varName = if (pat is stellaParser.PatternVarContext) pat.name.text else TODO()
            typeContext.addVariable(varName, ctx.rhs.accept(typeInfererVisitor))
            return varName
        }
    }

    inner class TypeInfererVisitor : stellaParserBaseVisitor<Type>() {
        private var expectedType: Type? = null

        fun expectType(expr: ParseTree, expectedType: Type): Type {
            if (this.expectedType != null) {
                val prevType = this.expectedType
                this.expectedType = expectedType
                expr.accept(this)
                this.expectedType = prevType
            } else {
                this.expectedType = expectedType
                expr.accept(this)
            }
            return expectedType
        }

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
            val tupleType = ctx.expr_.accept(this)
            if (tupleType !is TupleType) throw NotTuple(ctx.expr_, tupleType)
            val idx = ctx.index.text.toInt() - 1
            if (idx < 0 || idx >= tupleType.fieldsTypes.size) TODO()
            return tupleType.fieldsTypes[idx]
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext): Type {
            val recordType = ctx.expr_.accept(this)
            if (recordType !is RecordType) throw NotRecord(ctx.expr_, recordType)
            val label = ctx.label.text
            val type = recordType.fieldsTypes[label] ?: TODO()
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
            val vars = variablesDeclarator.declareAll(ctx.paramDecls)
            val paramsTypes = ctx.paramDecls.map { it.accept(this) }
            val retType = ctx.returnExpr.accept(this)
            typeContext.removeAllVariables(vars)
            return FunType(paramsTypes, retType)
        }

        override fun visitLet(ctx: stellaParser.LetContext): Type {
            val params = variablesDeclarator.declareAll(ctx.patternBindings)
            ctx.patternBindings.forEach { variablesDeclarator.declare(it) }
            val type = ctx.body.accept(this)
            typeContext.removeAllVariables(params)
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
            if (funType !is FunType) throw NotFunctionApplication(ctx.`fun`, funType)
            ctx.args.zip(funType.paramsTypes).map { (argExpr, paramType) -> expectType(argExpr, paramType) }
            return funType.retType
        }

        override fun visitIf(ctx: stellaParser.IfContext): Type {
            expectType(ctx.condition, BoolType)
            val thenType = ctx.thenExpr.accept(this)
            val elseType = ctx.elseExpr.accept(this)
            if (!isAssignable(thenType, elseType)) TODO()
            return thenType
        }

        private fun expectList(list: stellaParser.ExprContext): Type {
            val exprType = list.accept(this)
            if (exprType !is ListType) throw NotList(list, exprType)
            return exprType
        }

        override fun visitList(ctx: stellaParser.ListContext): Type {
            val expectedType = expectedType ?: TODO()
            if (expectedType !is ListType) throw UnexpectedList(ctx, expectedType)
            ctx.exprs.forEach { expectType(it, expectedType.elementsType) }
            return expectedType
        }

        override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext): Type {
            val expectedType = expectedType
            if(expectedType != null && !isAssignable(BoolType, expectedType)) TODO()
            expectList(ctx.list)
            return BoolType
        }

        override fun visitHead(ctx: stellaParser.HeadContext): Type {
            val expectedType = expectedType
            return if (expectedType != null)
                expectType(ctx.list, ListType(expectedType))
            else
                expectList(ctx.list)
        }

        override fun visitTail(ctx: stellaParser.TailContext): Type = expectList(ctx.list)
    }


}