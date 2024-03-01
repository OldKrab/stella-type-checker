package org.old.typecheck

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.old.grammar.UnimplementedStellaVisitor
import org.old.grammar.stellaParser

/***
 * Main class for type checking. Walk parse tree with context and infer or check expected types
 */
class TypeCheckerVisitor : UnimplementedStellaVisitor<Unit>("typecheck") {
    private val typeContext = TypeContext()

    /**
     * Walk type nodes and create inner type objects from them
     */
    private val typeConverterVisitor = object : UnimplementedStellaVisitor<Type>("convert type of") {

        override fun visitTypeParens(ctx: stellaParser.TypeParensContext): Type {
            return ctx.type_.accept(this)
        }

        override fun visitTypeNat(ctx: stellaParser.TypeNatContext): Type = NatType

        override fun visitTypeUnit(ctx: stellaParser.TypeUnitContext): Type = UnitType

        override fun visitTypeBool(ctx: stellaParser.TypeBoolContext): Type = BoolType

        override fun visitTypeFun(ctx: stellaParser.TypeFunContext): Type {
            val paramTypes = ctx.paramTypes.map(::convertType)
            val retType = convertType(ctx.returnType)
            return FunType(paramTypes, retType)
        }

        override fun visitTypeTuple(ctx: stellaParser.TypeTupleContext): Type {
            val fieldsTypes = ctx.types.map(::convertType)
            return TupleType(fieldsTypes)
        }

        override fun visitTypeRecord(ctx: stellaParser.TypeRecordContext): Type {
            val fieldsTypes = ctx.fieldTypes.associate { Pair(it.label.text, convertType(it.type_)) }
            return RecordType(fieldsTypes)
        }

        override fun visitTypeList(ctx: stellaParser.TypeListContext): Type {
            val elemType = convertType(ctx.type_)
            return ListType(elemType)
        }


        override fun visitTypeVariant(ctx: stellaParser.TypeVariantContext): Type {
            val fieldsTypes = ctx.fieldTypes.associate { Pair(it.label.text, convertType(it.type_)) }
            return VariantType(fieldsTypes)
        }
    }

    /**
     * Walk declaration nodes and add to typeContext declared variable. Return variable name to remove them further
     */
    private val variablesDeclarator = object : UnimplementedStellaVisitor<String>("declare variable in") {

        override fun visitDeclFun(ctx: stellaParser.DeclFunContext): String {
            val funName = ctx.name.text

            val paramsTypes = ctx.paramDecls.map { convertType(it.paramType) }
            val retType = convertType(ctx.returnType)
            val funType = FunType(paramsTypes, retType)
            typeContext.addVariable(funName, funType)
            return funName
        }

        override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): String {
            val varName = ctx.name.text
            typeContext.addVariable(varName, convertType(ctx.paramType))
            return varName
        }

        override fun visitPatternBinding(ctx: stellaParser.PatternBindingContext): String {
            val pat = ctx.pat
            val varName = if (pat is stellaParser.PatternVarContext) pat.name.text else TODO()
            typeContext.addVariable(varName, inferType(ctx.rhs))
            return varName
        }
    }

    /**
     * Walk parse tree and try to infer types of expressions
     */
    private val typeInfererVisitor = object : UnimplementedStellaVisitor<Type>("infer type of") {

        override fun visitVar(ctx: stellaParser.VarContext): Type {
            val varName = ctx.name.text
            val varType = typeContext.getVarType(varName) ?: throw UndefinedVariable(ctx)
            return varType
        }

        override fun visitTuple(ctx: stellaParser.TupleContext): Type {
            val paramsTypes = ctx.exprs.map(::inferType)
            return TupleType(paramsTypes)
        }

        override fun visitRecord(ctx: stellaParser.RecordContext): Type {
            val paramsTypes = ctx.bindings.associate { Pair(it.name.text, inferType(it.rhs)) }
            return RecordType(paramsTypes)
        }

        override fun visitTypeAsc(ctx: stellaParser.TypeAscContext): Type {
            val asType = convertType(ctx.type_)
            expectType(ctx.expr_, asType)
            return asType
        }

        override fun visitDotTuple(ctx: stellaParser.DotTupleContext): Type {
            val tupleType = inferType(ctx.expr_)
            if (tupleType !is TupleType) throw NotTuple(ctx.expr_, tupleType)
            val idx = ctx.index.text.toInt()
            if (idx <= 0 || idx > tupleType.fieldsTypes.size) throw TupleIndexOOB(ctx, tupleType, idx)
            return tupleType.fieldsTypes[idx - 1]
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext): Type {
            val recordType = inferType(ctx.expr_)
            if (recordType !is RecordType) throw NotRecord(ctx.expr_, recordType)
            val label = ctx.label.text
            val type = recordType.fieldsTypes[label] ?: throw UnexpectedFieldAccess(ctx, recordType, label)
            return type
        }

        override fun visitConstUnit(ctx: stellaParser.ConstUnitContext): Type = UnitType

        override fun visitSucc(ctx: stellaParser.SuccContext): Type {
            expectType(ctx.n, NatType)
            return NatType
        }

        override fun visitConstInt(ctx: stellaParser.ConstIntContext): Type = NatType
        override fun visitConstFalse(ctx: stellaParser.ConstFalseContext): Type = BoolType
        override fun visitConstTrue(ctx: stellaParser.ConstTrueContext): Type = BoolType

        override fun visitAbstraction(ctx: stellaParser.AbstractionContext): Type {
            val vars = declareAll(ctx.paramDecls)
            val paramsTypes = ctx.paramDecls.map { convertType(it.paramType) }
            val retType = inferType(ctx.returnExpr)
            typeContext.removeAllVariables(vars)
            return FunType(paramsTypes, retType)
        }

        override fun visitLet(ctx: stellaParser.LetContext): Type {
            val params = declareAll(ctx.patternBindings)
            val type = inferType(ctx.body)
            typeContext.removeAllVariables(params)
            return type
        }

        override fun visitNatRec(ctx: stellaParser.NatRecContext): Type {
            expectType(ctx.n, NatType)
            val initialType = inferType(ctx.initial)
            expectType(ctx.step, FunType(listOf(NatType), FunType(listOf(initialType), initialType)))
            return initialType
        }

        override fun visitApplication(ctx: stellaParser.ApplicationContext): Type {
            val funType = inferAnyFun(ctx.`fun`)
            // TODO compare args and params counts
            ctx.args.zip(funType.paramsTypes).forEach { (argExpr, paramType) -> expectType(argExpr, paramType) }
            return funType.retType
        }

        override fun visitIf(ctx: stellaParser.IfContext): Type {
            expectType(ctx.condition, BoolType)
            val thenType = inferType(ctx.thenExpr)
            expectType(ctx.elseExpr, thenType)
            return thenType
        }


        override fun visitList(ctx: stellaParser.ListContext): Type {
            if (ctx.exprs.size == 0) throw AmbiguousList(ctx) // can not infer without expected type
            val firstType = inferType(ctx.exprs[0])
            for (otherExpr in ctx.exprs.asSequence().drop(1)) expectType(otherExpr, firstType)
            return ListType(firstType)
        }

        override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext): BoolType {
            inferAnyList(ctx.list)
            return BoolType
        }

        override fun visitHead(ctx: stellaParser.HeadContext): Type {
            return inferAnyList(ctx.list).elementsType
        }

        override fun visitTail(ctx: stellaParser.TailContext): ListType = inferAnyList(ctx.list)

        override fun visitTerminatingSemicolon(ctx: stellaParser.TerminatingSemicolonContext): Type {
            return ctx.expr_.accept(this)
        }

        override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext): Type {
            return ctx.expr_.accept(this)
        }
    }

    /**
     * Walk parse tree and match expected type with actual
     */
    private val typeExpectVisitor = object : UnimplementedStellaVisitor<Unit>("expect type of") {
        private lateinit var expectedType: Type

        private fun throwIfNotExpected(ctx: ParserRuleContext, actualType: Type) {
            if (!isTypesEqual(actualType, expectedType)) throw UnexpectedExprType(ctx, expectedType, actualType)
        }

        fun expectType(expr: ParseTree, expectedType: Type): Type {
            // because antlr not generate parametrized visitor, we need store argument in field
            if (this::expectedType.isInitialized) {
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

        override fun visitVar(ctx: stellaParser.VarContext) {
            val varName = ctx.name.text
            val varType = typeContext.getVarType(varName) ?: throw UndefinedVariable(ctx)
            throwIfNotExpected(ctx, varType)
        }

        override fun visitTuple(ctx: stellaParser.TupleContext) {
            val expectedType = expectedType
            if (expectedType !is TupleType) throw UnexpectedTuple(ctx, expectedType)
            if (expectedType.fieldsTypes.size != ctx.exprs.size) throw UnexpectedTupleLength(
                ctx, ctx.exprs.size, expectedType.fieldsTypes.size
            )
            for ((expectedFieldType, fieldExpr) in expectedType.fieldsTypes.zip(ctx.exprs)) expectType(
                fieldExpr, expectedFieldType
            )
        }

        override fun visitRecord(ctx: stellaParser.RecordContext) {
            val expectedType = expectedType
            if (expectedType !is RecordType) throw UnexpectedRecord(ctx, expectedType)

            val declaredFields = ctx.bindings.map { it.name.text }.toSet()

            val unexpectedFields = declaredFields - expectedType.fieldsTypes.keys
            if (unexpectedFields.isNotEmpty()) throw UnexpectedRecordFields(ctx, expectedType, unexpectedFields)

            val missingFields = expectedType.fieldsTypes.keys - declaredFields
            if (missingFields.isNotEmpty()) throw MissingRecordFields(ctx, expectedType, missingFields)

            for ((expectedFieldType, fieldExpr) in expectedType.fieldsTypes.values.zip(ctx.bindings)) expectType(
                fieldExpr, expectedFieldType
            )
        }

        override fun visitTypeAsc(ctx: stellaParser.TypeAscContext) {
            val asType = convertType(ctx.type_)
            expectType(ctx.expr_, asType)
        }

        override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
            val fieldType = inferType(ctx)
            throwIfNotExpected(ctx, fieldType)
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
            val fieldType = inferType(ctx)
            throwIfNotExpected(ctx, fieldType)
        }

        override fun visitConstUnit(ctx: stellaParser.ConstUnitContext) {
            throwIfNotExpected(ctx, UnitType)
        }

        override fun visitSucc(ctx: stellaParser.SuccContext) {
            throwIfNotExpected(ctx, NatType)
            expectType(ctx.n, NatType)
        }

        override fun visitConstInt(ctx: stellaParser.ConstIntContext) {
            throwIfNotExpected(ctx, NatType)
        }

        override fun visitConstFalse(ctx: stellaParser.ConstFalseContext) {
            throwIfNotExpected(ctx, BoolType)
        }

        override fun visitConstTrue(ctx: stellaParser.ConstTrueContext) {
            throwIfNotExpected(ctx, BoolType)
        }

        override fun visitAbstraction(ctx: stellaParser.AbstractionContext) {
            val expectedType = expectedType
            if (expectedType !is FunType) throw UnexpectedLambda(ctx, expectedType)
            for ((expectedParamType, decl) in expectedType.paramsTypes.zip(ctx.paramDecls)) {
                val declType = convertType(decl.paramType)
                if (declType != expectedParamType) throw UnexpectedParamType(decl, expectedParamType, declType)
            }

            val params = declareAll(ctx.paramDecls)
            expectType(ctx.returnExpr, expectedType.retType)
            forgetAll(params)
        }

        override fun visitLet(ctx: stellaParser.LetContext) {
            val params = declareAll(ctx.patternBindings)
            expectType(ctx.body, expectedType)
            typeContext.removeAllVariables(params)
        }

        override fun visitNatRec(ctx: stellaParser.NatRecContext) {
            expectType(ctx.n, NatType)
            expectType(ctx.initial, expectedType)
            expectType(ctx.step, FunType(listOf(NatType), FunType(listOf(expectedType), expectedType)))
        }

        override fun visitApplication(ctx: stellaParser.ApplicationContext) {
            val funType = inferAnyFun(ctx.`fun`)
            funType.paramsTypes.zip(ctx.args).forEach { (type, arg) ->
                expectType(arg, type)
            }
            throwIfNotExpected(ctx, funType.retType)
        }

        override fun visitIf(ctx: stellaParser.IfContext) {
            expectType(ctx.condition, BoolType)
            expectType(ctx.thenExpr, expectedType)
            expectType(ctx.elseExpr, expectedType)
        }

        override fun visitList(ctx: stellaParser.ListContext) {
            val expectedType = expectedType
            if (expectedType !is ListType) throw UnexpectedList(ctx, expectedType)
            ctx.exprs.forEach { expectType(it, expectedType.elementsType) }
        }

        override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext) {
            inferAnyList(ctx.list)
            throwIfNotExpected(ctx, BoolType)
        }

        override fun visitHead(ctx: stellaParser.HeadContext) {
            expectType(ctx.list, ListType(expectedType))
        }

        override fun visitTail(ctx: stellaParser.TailContext) {
            if (expectedType !is ListType) {
                val argType = inferAnyList(ctx.list)
                throw UnexpectedExprType(ctx, expectedType, argType)
            }
            expectType(ctx.list, expectedType)
        }

        override fun visitIsZero(ctx: stellaParser.IsZeroContext) {
            expectType(ctx.n, NatType)
            throwIfNotExpected(ctx, BoolType)
        }

        override fun visitBinding(ctx: stellaParser.BindingContext) {
            expectType(ctx.rhs, expectedType)
        }

        override fun visitTerminatingSemicolon(ctx: stellaParser.TerminatingSemicolonContext) {
            ctx.expr_.accept(this)
        }

        override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext) {
            ctx.expr_.accept(this)
        }

        override fun visitFix(ctx: stellaParser.FixContext) {
            expectType(ctx.expr_, FunType(listOf(expectedType), expectedType))
        }

        override fun visitVariant(ctx: stellaParser.VariantContext) {
            val expectedType = expectedType
            if (expectedType !is VariantType) throw UnexpectedVariant(ctx, expectedType)
            val label = ctx.label.text
            val variantType = expectedType.variantsTypes[label] ?: throw UnexpectedVariantLabel(ctx, expectedType, label)
            expectType(ctx.rhs, variantType)
        }
    }

    fun expectType(expr: ParseTree, expectedType: Type): Type {
        return typeExpectVisitor.expectType(expr, expectedType)
    }

    fun inferAnyFun(expr: ParserRuleContext): FunType {
        val funType = inferType(expr)
        if (funType !is FunType) throw NotFunctionApplication(expr, funType)
        return funType
    }

    private fun inferAnyList(list: stellaParser.ExprContext): ListType {
        val exprType = inferType(list)
        if (exprType !is ListType) throw NotList(list, exprType)
        return exprType
    }

    fun inferType(expr: ParseTree): Type {
        return expr.accept(typeInfererVisitor)
    }

    fun convertType(expr: ParseTree): Type {
        return expr.accept(typeConverterVisitor)
    }

    fun declare(ctx: ParseTree): String {
        return ctx.accept(variablesDeclarator)
    }

    fun declareAll(trees: Iterable<ParseTree>): List<String> {
        return trees.map { declare(it) }
    }

    fun forgetAll(vars: Iterable<String>) {
        typeContext.removeAllVariables(vars)
    }

    override fun visitStart_Program(ctx: stellaParser.Start_ProgramContext): Unit = ctx.program().accept(this)

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        ctx.decls.forEach { it.accept(this) }

        val isMainContains = ctx.decls.filterIsInstance<stellaParser.DeclFunContext>().any { it.name.text == "main" }
        if (!isMainContains) throw MainMissing()
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        declare(ctx)
        val vars = declareAll(ctx.paramDecls) + declareAll(ctx.localDecls)

        val declReturnType = convertType(ctx.returnType)
        expectType(ctx.returnExpr, declReturnType)

        forgetAll(vars)
    }

}