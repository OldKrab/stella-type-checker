package org.old.typecheck

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.old.grammar.ParametrizedVisitor
import org.old.grammar.UnimplementedStellaVisitor
import org.old.grammar.stellaParser
import org.old.grammar.stellaParser.MatchCaseContext
import org.old.grammar.stellaParser.MatchContext
import org.old.grammar.stellaParser.PatternBindingContext
import org.old.grammar.stellaParser.PatternConsContext
import org.old.grammar.stellaParser.PatternContext
import org.old.grammar.stellaParser.PatternTrueContext
import org.old.grammar.stellaParser.PatternFalseContext
import org.old.grammar.stellaParser.PatternListContext
import org.old.grammar.stellaParser.PatternVarContext


/***
 * Main class for type checking. Walk parse tree with context and infer or check expected types
 */
class TypeCheckerVisitor : UnimplementedStellaVisitor<Unit>("typecheck") {
    private val typeContext = TypeContext()

    /**
     * Walk type nodes and create inner type objects from them
     */
    private val typeConverterVisitor = object : UnimplementedStellaVisitor<Type>("convert type of") {
        override fun visitTypeTop(ctx: stellaParser.TypeTopContext): Type = TopType

        override fun visitTypeBottom(ctx: stellaParser.TypeBottomContext): Type = BotType

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
            val fields = ctx.fieldTypes.map { it.label.text }
            val fieldsTypes = ctx.fieldTypes.associate { Pair(it.label.text, convertType(it.type_)) }
            return RecordType(fields, fieldsTypes)
        }

        override fun visitTypeList(ctx: stellaParser.TypeListContext): Type {
            val elemType = convertType(ctx.type_)
            return ListType(elemType)
        }

        override fun visitTypeVariant(ctx: stellaParser.TypeVariantContext): Type {
            val fields = ctx.fieldTypes.map { it.label.text }
            val fieldsTypes =
                ctx.fieldTypes.associate { field -> Pair(field.label.text, field.type_?.let { convertType(it) }) }
            return VariantType(fields, fieldsTypes)
        }

        override fun visitTypeSum(ctx: stellaParser.TypeSumContext): Type {
            return SumType(convertType(ctx.left), convertType(ctx.right))
        }

        override fun visitTypeRef(ctx: stellaParser.TypeRefContext): Type {
            return RefType(convertType(ctx.type_))
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

            val vars = declareAll(ctx.paramDecls) + declareAll(ctx.localDecls)
            expectType(ctx.returnExpr, retType)
            forgetAll(vars)

            return funName
        }

        override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): String {
            val varName = ctx.name.text
            typeContext.addVariable(varName, convertType(ctx.paramType))
            return varName
        }

    }

    /**
     * Walk pattern and add to typeContext declared variable. Return variables names to remove them further. Also checks patterns types
     */
    private val patternVariablesDeclarator =
        object : ParametrizedVisitor<Type?, List<String>>("declare pattern variables in") {
            val expectedType
                get() = arg

            override fun visitPatternAsc(ctx: stellaParser.PatternAscContext): List<String> {
                val expectedType = this.expectedType
                val asType = convertType(ctx.type_)
                val vars = declarePattern(ctx.pattern_, asType)
                if (expectedType != null && expectedType != asType)
                    throw UnexpectedPatternForType(ctx, expectedType)
                return vars
            }

            override fun visitPatternInl(ctx: stellaParser.PatternInlContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType is SumType)
                    return declarePattern(ctx.pattern_, expectedType.inl)
                if (expectedType == null)
                    return declarePattern(ctx.pattern_, null)
                throw UnexpectedPatternForType(ctx, expectedType)
            }

            override fun visitPatternInr(ctx: stellaParser.PatternInrContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType is SumType)
                    return declarePattern(ctx.pattern_, expectedType.inr)
                if (expectedType == null)
                    return declarePattern(ctx.pattern_, null)
                throw UnexpectedPatternForType(ctx, expectedType)
            }

            override fun visitPatternVar(ctx: PatternVarContext): List<String> {
                val varName = ctx.text
                val expectedType = this.expectedType ?: throw AmbiguousPatternType(ctx)
                typeContext.addVariable(varName, expectedType)
                return listOf(varName)
            }

            override fun visitPatternVariant(ctx: stellaParser.PatternVariantContext): List<String> {
                val expectedType = this.expectedType
                val variantLabel = ctx.label.text
                if (expectedType is VariantType) {
                    if (!expectedType.fields.contains(variantLabel))
                        throw UnexpectedPatternForType(ctx, expectedType)
                    val expectedVariantType = expectedType.variantsTypes[variantLabel]
                    if (ctx.pattern_ == null && expectedVariantType != null)
                        throw UnexpectedNullaryVariantPattern(ctx, expectedType, variantLabel)
                    if (ctx.pattern_ != null && expectedVariantType == null)
                        throw UnexpectedNonNullaryVariantPattern(ctx.pattern_, expectedType, variantLabel)
                    if (ctx.pattern_ != null && expectedVariantType != null)
                        return declarePattern(ctx.pattern_, expectedVariantType)
                    return emptyList() // Both nullary
                }
                if (expectedType != null)
                    throw UnexpectedPatternForType(ctx, expectedType)
                if (ctx.pattern_ != null)
                    return declarePattern(ctx.pattern_, null)
                return emptyList()
            }

            fun throwIfNotExpected(ctx: PatternContext, actualType: Type) {
                val expectedType = this.expectedType
                if (expectedType != null) {
                    if (expectedType != actualType)
                        throw UnexpectedPatternForType(ctx, expectedType)
                }
            }

            override fun visitPatternTrue(ctx: PatternTrueContext): List<String> {
                throwIfNotExpected(ctx, BoolType)
                return emptyList()
            }

            override fun visitPatternFalse(ctx: PatternFalseContext): List<String> {
                throwIfNotExpected(ctx, BoolType)
                return emptyList()
            }

            override fun visitPatternUnit(ctx: stellaParser.PatternUnitContext): List<String> {
                throwIfNotExpected(ctx, UnitType)
                return emptyList()
            }

            override fun visitPatternInt(ctx: stellaParser.PatternIntContext): List<String> {
                throwIfNotExpected(ctx, NatType)
                return emptyList()
            }

            override fun visitPatternList(ctx: PatternListContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType != null && expectedType !is ListType)
                    throw UnexpectedPatternForType(ctx, expectedType)
                val elementType = (expectedType as ListType?)?.elementsType
                return ctx.patterns.flatMap { declarePattern(it, elementType) }
            }

            override fun visitPatternSucc(ctx: stellaParser.PatternSuccContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType != null && expectedType != NatType)
                    throw UnexpectedPatternForType(ctx, expectedType)
                return declarePattern(ctx.pattern_, expectedType)
            }

            override fun visitPatternCons(ctx: PatternConsContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType != null && expectedType !is ListType)
                    throw UnexpectedPatternForType(ctx, expectedType)
                val elementType = (expectedType as ListType?)?.elementsType
                return declarePattern(ctx.head, elementType) + declarePattern(ctx.tail, expectedType)
            }

            override fun visitPatternRecord(ctx: stellaParser.PatternRecordContext): List<String> {
                val expectedType = this.expectedType
                val labelsPats = ctx.patterns.associate { it.label.text to it.pattern_ }
                if (expectedType != null) {
                    if (expectedType !is RecordType)
                        throw UnexpectedPatternForType(ctx, expectedType)

                    if (labelsPats.keys != expectedType.labels.toSet())
                        throw UnexpectedPatternForType(ctx, expectedType)
                    return expectedType.labels.flatMap {
                        declarePattern(
                            labelsPats[it]!!,
                            expectedType.fieldsTypes[it]!!
                        )
                    }
                }
                return labelsPats.values.flatMap { declarePattern(it, null) }

            }

            override fun visitPatternTuple(ctx: stellaParser.PatternTupleContext): List<String> {
                val expectedType = this.expectedType
                if (expectedType != null) {
                    if (expectedType !is TupleType)
                        throw UnexpectedPatternForType(ctx, expectedType)
                    if (expectedType.fieldsTypes.size != ctx.patterns.size)
                        throw UnexpectedPatternForType(ctx, expectedType)
                    return expectedType.fieldsTypes.withIndex()
                        .flatMap { (i, fType) -> declarePattern(ctx.patterns[i], fType) }
                }
                return ctx.patterns.flatMap { declarePattern(it, null) }
            }

            override fun visitPatternCastAs(ctx: stellaParser.PatternCastAsContext): List<String> {
                val castType = convertType(ctx.type_)
                if (typeContext.isSubTypingEnabled)
                    expectedType?.let { checkSubTypeOf(ctx, castType, it) }
                else
                    throwIfNotExpected(ctx, castType)
                return declarePattern(ctx.pattern_, castType)
            }
        }


    /**
     * Walk parse tree and try to infer types of expressions
     */
    private val typeInfererVisitor = object : UnimplementedStellaVisitor<Type>("infer type of") {

        override fun visitLetRec(ctx: stellaParser.LetRecContext): Type {
            val params = declareLetBindingsWithoutTypeCheck(ctx.patternBindings)
            val type = inferType(ctx.body)
            forgetAll(params)
            return type
        }

        override fun visitIsZero(ctx: stellaParser.IsZeroContext): Type {
            expectType(ctx.n, NatType)
            return BoolType
        }

        override fun visitVariant(ctx: stellaParser.VariantContext): Type {
            if (typeContext.isSubTypingEnabled) {
                val label = ctx.label.text
                val type = inferType(ctx.rhs)
                return VariantType(listOf(label), mapOf(label to type))
            }
            throw AmbiguousVariantType(ctx)
        }

        override fun visitMatch(ctx: MatchContext): Type {
            return visitMatch(ctx) { exprType ->
                val firstType = visitMatchCase(ctx.cases[0], exprType) {
                    inferType(ctx.cases[0].expr_)
                }
                for (case in ctx.cases.drop(1)) {
                    visitMatchCase(case, exprType) {
                        expectType(case.expr_, firstType)
                    }
                }
                firstType
            }
        }

        override fun visitFix(ctx: stellaParser.FixContext): Type {
            val func = inferType(ctx.expr_)
            if (func !is FunType || func.paramsTypes.size != 1)
                throw NotFunction(ctx.expr_, func)
            val paramType = func.paramsTypes[0]
            if (paramType != func.retType)
                throw UnexpectedExprType(ctx.expr_, FunType(listOf(paramType), paramType), func)
            return paramType
        }

        override fun visitConsList(ctx: stellaParser.ConsListContext): Type {
            val elemType = inferType(ctx.head)
            expectType(ctx.tail, ListType(elemType))
            return ListType(elemType)
        }

        override fun visitVar(ctx: stellaParser.VarContext): Type {
            return getVarType(ctx)
        }

        override fun visitTuple(ctx: stellaParser.TupleContext): Type {
            val paramsTypes = ctx.exprs.map(::inferType)
            return TupleType(paramsTypes)
        }

        override fun visitRecord(ctx: stellaParser.RecordContext): Type {
            val params = ctx.bindings.map { it.name.text }
            val paramsTypes = ctx.bindings.associate { Pair(it.name.text, inferType(it.rhs)) }
            return RecordType(params, paramsTypes)
        }

        override fun visitTypeAsc(ctx: stellaParser.TypeAscContext): Type {
            val asType = convertType(ctx.type_)
            expectType(ctx.expr_, asType)
            return asType
        }

        override fun visitDotTuple(ctx: stellaParser.DotTupleContext): Type {
            val tupleType = inferType(ctx.expr_)
            if (tupleType !is TupleType)
                throw NotTuple(ctx.expr_, tupleType)
            val idx = ctx.index.text.toInt()
            if (idx <= 0 || idx > tupleType.fieldsTypes.size)
                throw TupleIndexOOB(ctx, tupleType, idx)
            return tupleType.fieldsTypes[idx - 1]
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext): Type {
            val recordType = inferType(ctx.expr_)
            if (recordType !is RecordType)
                throw NotRecord(ctx.expr_, recordType)
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
            val params = declareAll(ctx.paramDecls)
            val paramsTypes = ctx.paramDecls.map { convertType(it.paramType) }
            val retType = inferType(ctx.returnExpr)
            forgetAll(params)
            return FunType(paramsTypes, retType)
        }

        override fun visitLet(ctx: stellaParser.LetContext): Type {
            val params = declareLetBindings(ctx.patternBindings)
            val type = inferType(ctx.body)
            forgetAll(params)
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
            if (ctx.args.size != funType.paramsTypes.size)
                throw IncorrectNumberOfArguments(ctx, funType.paramsTypes.size, ctx.args.size)
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
            if (ctx.exprs.size == 0) {
                if (typeContext.isAmbiguousTypeAsBottom) return ListType(BotType)
                throw AmbiguousList(ctx) // can not infer without expected type
            }
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


        override fun visitInl(ctx: stellaParser.InlContext): Type {
            if (typeContext.isAmbiguousTypeAsBottom && typeContext.isSubTypingEnabled) {
                val t = inferType(ctx.expr_)
                return SumType(t, BotType)
            }
            throw AmbiguousSumType(ctx)
        }

        override fun visitInr(ctx: stellaParser.InrContext): Type {
            if (typeContext.isAmbiguousTypeAsBottom && typeContext.isSubTypingEnabled) {
                val t = inferType(ctx.expr_)
                return SumType(BotType, t)
            }
            throw AmbiguousSumType(ctx)
        }

        override fun visitSequence(ctx: stellaParser.SequenceContext): Type {
            expectType(ctx.expr1, UnitType)
            return inferType(ctx.expr2)
        }

        override fun visitAssign(ctx: stellaParser.AssignContext): Type {
            val ref = inferAnyRef(ctx.lhs)
            expectType(ctx.rhs, ref.inner)
            return UnitType
        }

        override fun visitDeref(ctx: stellaParser.DerefContext): Type {
            val ref = inferAnyRef(ctx.expr_)
            return ref.inner
        }

        override fun visitRef(ctx: stellaParser.RefContext): Type {
            val t = inferType(ctx.expr_)
            return RefType(t)
        }

        override fun visitConstMemory(ctx: stellaParser.ConstMemoryContext): Type {
            throw AmbiguousRef(ctx)
        }

        override fun visitPanic(ctx: stellaParser.PanicContext): Type {
            if (typeContext.isAmbiguousTypeAsBottom) return BotType
            throw AmbiguousPanic(ctx)
        }

        override fun visitThrow(ctx: stellaParser.ThrowContext): Type {
            if (typeContext.isAmbiguousTypeAsBottom) return BotType
            throw AmbiguousThrow(ctx)
        }

        override fun visitTryCatch(ctx: stellaParser.TryCatchContext): Type {
            val t = inferType(ctx.tryExpr)
            visitCatch(ctx) {
                expectType(ctx.fallbackExpr, t)
            }
            return t
        }

        override fun visitTryWith(ctx: stellaParser.TryWithContext): Type {
            val t = inferType(ctx.tryExpr)
            expectType(ctx.fallbackExpr, t)
            return t
        }

        override fun visitTypeCast(ctx: stellaParser.TypeCastContext): Type {
            inferType(ctx.expr_)
            return convertType(ctx.type_)
        }

        override fun visitTryCastAs(ctx: stellaParser.TryCastAsContext): Type {
            inferType(ctx.tryExpr)
            val t = convertType(ctx.type_)
            return visitTryWithPattern(ctx.pattern_, t) {
                val exprT = inferType(ctx.expr_)
                expectType(ctx.fallbackExpr, exprT)
                exprT
            }
        }
    }


    /**
     * Walk parse tree and match expected type with actual
     */
    private val typeExpectVisitor = object : ParametrizedVisitor<Type, Unit>("expect type of") {
        val expectedType
            get() = arg!!


        private fun throwIfNotExpected(ctx: ParserRuleContext, actualType: Type) {
            if (typeContext.isSubTypingEnabled)
                checkSubTypeOf(ctx, actualType, expectedType)
            else if (actualType != expectedType)
                throw UnexpectedExprType(ctx, expectedType, actualType)
        }

        override fun visitConsList(ctx: stellaParser.ConsListContext) {
            val expectedType = expectedType
            if (expectedType is ListType) {
                expectType(ctx.head, expectedType.elementsType)
                expectType(ctx.tail, expectedType)
            } else if (expectedType is TopType) {
                expectType(ctx.head, TopType)
                expectType(ctx.tail, TopType)
            } else {
                throw UnexpectedList(ctx, expectedType)
            }

        }

        override fun visitVar(ctx: stellaParser.VarContext) {
            val varType = getVarType(ctx)
            throwIfNotExpected(ctx, varType)
        }

        override fun visitTuple(ctx: stellaParser.TupleContext) {
            val expectedType = expectedType
            if (expectedType !is TupleType)
                throw UnexpectedTuple(ctx, expectedType)
            if (expectedType.fieldsTypes.size != ctx.exprs.size)
                throw UnexpectedTupleLength(ctx, ctx.exprs.size, expectedType.fieldsTypes.size)
            for ((expectedFieldType, fieldExpr) in expectedType.fieldsTypes.zip(ctx.exprs))
                expectType(fieldExpr, expectedFieldType)
        }

        override fun visitRecord(ctx: stellaParser.RecordContext) {
            val expectedType = expectedType
            if (expectedType !is RecordType)
                throw UnexpectedRecord(ctx, expectedType)

            val declaredFields = ctx.bindings.map { it.name.text }.toSet()
            if (!typeContext.isSubTypingEnabled) {
                val unexpectedFields = declaredFields - expectedType.fieldsTypes.keys
                if (unexpectedFields.isNotEmpty())
                    throw UnexpectedRecordFields(ctx, expectedType, unexpectedFields)
            }

            checkMissingRecordFields(ctx, declaredFields, expectedType)

            for (declaredField in ctx.bindings) {
                val fieldName = declaredField.name.text
                if (fieldName in expectedType.fieldsTypes)
                    expectType(declaredField.rhs, expectedType.fieldsTypes[declaredField.name.text]!!)
                else
                    expectType(declaredField.rhs, TopType)
            }
        }

        override fun visitTypeAsc(ctx: stellaParser.TypeAscContext) {
            throwIfNotExpected(ctx, inferType(ctx))
        }

        override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
            throwIfNotExpected(ctx, inferType(ctx))
        }

        override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
            throwIfNotExpected(ctx, inferType(ctx))
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
            if (expectedType !is FunType)
                throw UnexpectedLambda(ctx, expectedType)
            if (expectedType.paramsTypes.size != ctx.paramDecls.size)
                throw UnexpectedNumberOfLambdaParameters(ctx, expectedType.paramsTypes.size, ctx.paramDecls.size)
            for ((expectedParamType, decl) in expectedType.paramsTypes.zip(ctx.paramDecls)) {
                val declType = convertType(decl.paramType)
                if (typeContext.isSubTypingEnabled) {
                    checkSubTypeOf(decl, expectedParamType, declType)
                } else if (expectedParamType != declType)
                    throw UnexpectedParamType(decl, expectedParamType, declType)
            }
            val params = declareAll(ctx.paramDecls)
            expectType(ctx.returnExpr, expectedType.retType)
            forgetAll(params)
        }

        override fun visitLetRec(ctx: stellaParser.LetRecContext) {
            val params = declareLetBindingsWithoutTypeCheck(ctx.patternBindings)
            expectType(ctx.body, expectedType)
            forgetAll(params)
        }

        override fun visitLet(ctx: stellaParser.LetContext) {
            val params = declareLetBindings(ctx.patternBindings)
            expectType(ctx.body, expectedType)
            forgetAll(params)
        }

        override fun visitNatRec(ctx: stellaParser.NatRecContext) {
            expectType(ctx.n, NatType)
            expectType(ctx.initial, expectedType)
            expectType(ctx.step, FunType(listOf(NatType), FunType(listOf(expectedType), expectedType)))
        }

        override fun visitApplication(ctx: stellaParser.ApplicationContext) {
            val retType = inferType(ctx)
            throwIfNotExpected(ctx, retType)
        }

        override fun visitIf(ctx: stellaParser.IfContext) {
            expectType(ctx.condition, BoolType)
            expectType(ctx.thenExpr, expectedType)
            expectType(ctx.elseExpr, expectedType)
        }

        override fun visitList(ctx: stellaParser.ListContext) {
            val expectedType = expectedType
            if (expectedType is ListType) {
                ctx.exprs.forEach { expectType(it, expectedType.elementsType) }
            } else if (expectedType is TopType) {
                ctx.exprs.forEach { expectType(it, TopType) }
            } else {
                throw UnexpectedList(ctx, expectedType)
            }
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

        override fun visitFix(ctx: stellaParser.FixContext) {
            expectType(ctx.expr_, FunType(listOf(expectedType), expectedType))
        }


        override fun visitVariant(ctx: stellaParser.VariantContext) {
            val expectedType = expectedType
            if (expectedType !is VariantType)
                throw UnexpectedVariant(ctx, expectedType)
            val label = ctx.label.text
            if (!expectedType.variantsTypes.containsKey(label))
                throw UnexpectedVariantLabel(ctx, expectedType, label)
            val expectedFieldType = expectedType.variantsTypes.getValue(label)
            if (ctx.rhs == null && expectedFieldType != null)
                throw MissingDataForLabel(ctx, expectedType, expectedFieldType, label)
            if (ctx.rhs != null && expectedFieldType == null)
                throw UnexpectedDataForNullaryLabel(ctx, expectedType, label)
            if (ctx.rhs != null && expectedFieldType != null)
                expectType(ctx.rhs, expectedFieldType)
        }

        override fun visitInl(ctx: stellaParser.InlContext) {
            val expectedType = expectedType
            if (expectedType !is SumType)
                throw UnexpectedInjection(ctx, expectedType)
            expectType(ctx.expr_, expectedType.inl)
        }

        override fun visitInr(ctx: stellaParser.InrContext) {
            val expectedType = expectedType
            if (expectedType !is SumType)
                throw UnexpectedInjection(ctx, expectedType)
            expectType(ctx.expr_, expectedType.inr)
        }

        override fun visitMatch(ctx: MatchContext) {
            visitMatch(ctx) { exprType ->
                for (case in ctx.cases) {
                    visitMatchCase(case, exprType) {
                        expectType(case.expr_, expectedType)
                    }
                }
            }
        }

        override fun visitSequence(ctx: stellaParser.SequenceContext) {
            expectType(ctx.expr1, UnitType)
            expectType(ctx.expr2, expectedType)
        }

        override fun visitAssign(ctx: stellaParser.AssignContext) {
            throwIfNotExpected(ctx, inferType(ctx))
        }


        override fun visitDeref(ctx: stellaParser.DerefContext) {
            expectType(ctx.expr_, RefType(expectedType))
        }

        override fun visitRef(ctx: stellaParser.RefContext) {
            val expectedRef = expectedType
            if (expectedRef is RefType) {
                expectType(ctx.expr_, expectedRef.inner)
            } else if (expectedType is TopType) {
                expectType(ctx.expr_, TopType)
            } else {
                throw UnexpectedReference(ctx, expectedRef)

            }
        }

        override fun visitConstMemory(ctx: stellaParser.ConstMemoryContext) {
            if (expectedType !is RefType && expectedType !is TopType) {
                throw UnexpectedMemoryAddress(ctx, expectedType)
            }
        }

        override fun visitPanic(ctx: stellaParser.PanicContext) {
        }

        override fun visitThrow(ctx: stellaParser.ThrowContext) {
            val exceptionsType = typeContext.exceptionsType ?: throw ExceptionTypeNotDeclared(ctx.expr_)
            expectType(ctx.expr_, exceptionsType)
        }

        override fun visitTryCatch(ctx: stellaParser.TryCatchContext) {
            expectType(ctx.tryExpr, expectedType)
            visitCatch(ctx) {
                expectType(ctx.fallbackExpr, expectedType)
            }
        }

        override fun visitTryWith(ctx: stellaParser.TryWithContext) {
            expectType(ctx.tryExpr, expectedType)
            expectType(ctx.fallbackExpr, expectedType)
        }

        override fun visitTypeCast(ctx: stellaParser.TypeCastContext) {
            inferType(ctx.expr_)
        }


        override fun visitTryCastAs(ctx: stellaParser.TryCastAsContext) {
            inferType(ctx.tryExpr)
            val t = convertType(ctx.type_)
            visitTryWithPattern(ctx.pattern_, t) {
                expectType(ctx.expr_, expectedType)
                expectType(ctx.fallbackExpr, expectedType)
            }
        }
    }

    fun getVarType(ctx: stellaParser.VarContext): Type {
        val varName = ctx.name.text
        return typeContext.getVarType(varName) ?: throw UndefinedVariable(ctx)
    }

    fun expectType(expr: ParseTree, expectedType: Type) {
        typeExpectVisitor.visitWithArg(expr, expectedType)
    }

    fun inferAnyFun(expr: ParserRuleContext): FunType {
        val funType = inferType(expr)
        if (funType !is FunType)
            throw NotFunction(expr, funType)
        return funType
    }

    private fun inferAnyList(list: stellaParser.ExprContext): ListType {
        val exprType = inferType(list)
        if (exprType !is ListType)
            throw NotList(list, exprType)
        return exprType
    }

    private fun inferAnyRef(ref: stellaParser.ExprContext): RefType {
        val exprType = inferType(ref)
        if (exprType !is RefType)
            throw NotRef(ref, exprType)
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

    fun declareLetBindings(patternBindings: List<PatternBindingContext>): List<String> {
        return patternBindings.flatMap {
            val rhsType = inferType(it.rhs)
            val params = declarePattern(it.pat, rhsType)
            if (!checkExhaustivePatterns(rhsType, listOf(it.pat)))
                throw NonExhaustiveLetPatterns(it)
            params
        }
    }


    fun checkMissingRecordFields(ctx: ParserRuleContext, subFields: Set<String>, baseType: RecordType) {
        val missingFields = baseType.fieldsTypes.keys - subFields
        if (missingFields.isNotEmpty())
            throw MissingRecordFields(ctx, baseType, missingFields)
    }


    fun checkVariantsSubTyping(ctx: ParserRuleContext, subType: VariantType, baseType: VariantType) {
        val extraFields = subType.variantsTypes.keys - baseType.variantsTypes.keys
        if (extraFields.isNotEmpty()) throw UnexpectedVariantLabel(ctx, baseType, extraFields.first())
        for ((subFieldName, subFieldType) in subType.variantsTypes.entries) {
            val baseFieldType = baseType.variantsTypes.getValue(subFieldName)
            val baseLabelType = baseType.variantsTypes[subFieldName]
            if (subFieldType == null && baseLabelType != null)
                throw MissingTypeForLabel(ctx, baseType, baseLabelType, subFieldName)
            if (subFieldType != null && baseLabelType == null)
                throw UnexpectedTypeForNullaryLabel(ctx, baseType, subFieldName)
            if (subFieldType != null && baseFieldType != null)
                checkSubTypeOf(ctx, subFieldType, baseFieldType)
        }
    }

    fun checkTupleSubTyping(ctx: ParserRuleContext, subType: TupleType, baseType: TupleType) {
        if (baseType.fieldsTypes.size != subType.fieldsTypes.size)
            throw UnexpectedTupleLength(ctx, subType.fieldsTypes.size, baseType.fieldsTypes.size)
        for ((baseFieldType, subFieldType) in baseType.fieldsTypes.zip(subType.fieldsTypes))
            checkSubTypeOf(ctx, subFieldType, baseFieldType)
    }

    fun checkFuncSubType(ctx: ParserRuleContext, subType: FunType, baseType: FunType) {
        checkSubTypeOf(ctx, subType.retType, baseType.retType)
        if (baseType.paramsTypes.size != subType.paramsTypes.size) throw IncorrectNumberOfArguments(
            ctx,
            baseType.paramsTypes.size,
            subType.paramsTypes.size
        )
        baseType.paramsTypes.zip(subType.paramsTypes)
            .forEach { (baseParam, subParam) -> checkSubTypeOf(ctx, baseParam, subParam) }
    }

    fun checkSubTypeOf(ctx: ParserRuleContext, subType: Type, baseType: Type) {
        if (baseType is TopType || subType is BotType) return
        else if (subType is RecordType && baseType is RecordType) {
            checkMissingRecordFields(ctx, subType.fieldsTypes.keys, baseType)
            baseType.fieldsTypes.entries.forEach { (baseFieldName, baseFieldType) ->
                checkSubTypeOf(ctx, subType.fieldsTypes.getValue(baseFieldName), baseFieldType)
            }
        } else if (subType is VariantType && baseType is VariantType) {
            checkVariantsSubTyping(ctx, subType, baseType)
        } else if (subType is FunType && baseType is FunType) {
            checkFuncSubType(ctx, subType, baseType)
        } else if (subType is TupleType && baseType is TupleType) {
            checkTupleSubTyping(ctx, subType, baseType)
        } else if (subType is SumType && baseType is SumType) {
            checkSubTypeOf(ctx, subType.inl, baseType.inl)
            checkSubTypeOf(ctx, subType.inr, baseType.inr)
        } else if (subType != baseType)
            throw UnexpectedSubType(ctx, baseType, subType)
    }

    fun declareLetBindingsWithoutTypeCheck(patternBindings: List<PatternBindingContext>): List<String> {
        return patternBindings.flatMap {
            val params = declarePattern(it.pat, null)
            val rhsType = inferType(it.rhs)
            declarePattern(it.pat, rhsType)

            if (!checkExhaustivePatterns(rhsType, listOf(it.pat)))
                throw NonExhaustiveLetPatterns(it)
            params
        }
    }

    fun declarePattern(ctx: ParseTree, expectedType: Type?): List<String> {
        return patternVariablesDeclarator.visitWithArg(ctx, expectedType)
    }

    fun forgetAll(vars: Iterable<String>) {
        typeContext.removeAllVariables(vars)
    }

    fun <T> visitMatch(ctx: MatchContext, action: (Type) -> T): T {
        if (ctx.cases.size == 0)
            throw IllegalEmptyMatching(ctx)
        val exprType = inferType(ctx.expr_)
        val res = action(exprType)
        if (!checkExhaustivePatterns(exprType, ctx.cases.map { it.pattern_ }))
            throw NonExhaustiveMatchPatterns(ctx)
        return res
    }

    fun <T> visitMatchCase(case: MatchCaseContext, exprType: Type, action: () -> T): T {
        val vars = declarePattern(case.pattern_, exprType)
        val res = action()
        forgetAll(vars)
        return res
    }

    fun <T> visitCatch(ctx: stellaParser.TryCatchContext, action: () -> T): T {
        val exceptionsType = typeContext.exceptionsType ?: throw ExceptionTypeNotDeclared(ctx.pat)
        return visitTryWithPattern(ctx.pat, exceptionsType, action)
    }

    fun <T> visitTryWithPattern(pat: PatternContext, patType: Type, action: () -> T): T {
        val vars = declarePattern(pat, patType)
        val res = action()
        forgetAll(vars)
        return res
    }

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        fun hasExtension(ext: String): Boolean {
            return ctx.extensions
                .filterIsInstance<stellaParser.AnExtensionContext>()
                .any { anExt -> anExt.extensionNames.any { it.text == ext } }
        }
        typeContext.isSubTypingEnabled = hasExtension("#structural-subtyping")
        typeContext.isAmbiguousTypeAsBottom = hasExtension("#ambiguous-type-as-bottom")
        ctx.decls.forEach { it.accept(this) }
        val main = ctx.decls.filterIsInstance<stellaParser.DeclFunContext>().firstOrNull { it.name.text == "main" }
        if (main == null)
            throw MainMissing()
        if (main.paramDecls.size != 1)
            throw IncorrectArityOfMain(main)
    }

    override fun visitDeclExceptionType(ctx: stellaParser.DeclExceptionTypeContext) {
        typeContext.exceptionsType = convertType(ctx.exceptionType)
    }

    override fun visitDeclExceptionVariant(ctx: stellaParser.DeclExceptionVariantContext) {
        val label = ctx.name.text
        val type = convertType(ctx.variantType)
        val exType = typeContext.exceptionsType
        if (exType is VariantType) {
            typeContext.exceptionsType = VariantType(exType.fields + label, exType.variantsTypes + (label to type))
        } else {
            typeContext.exceptionsType = VariantType(listOf(label), mapOf(label to type))
        }

    }

    // Top level functions declarations
    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        declare(ctx)
    }

}