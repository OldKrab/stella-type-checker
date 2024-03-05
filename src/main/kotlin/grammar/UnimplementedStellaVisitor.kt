package org.old.grammar

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor
import org.old.typecheck.Type

abstract class UnimplementedStellaVisitor<T>(private val action: String) : AbstractParseTreeVisitor<T>(),
    stellaParserVisitor<T> {


    override fun visitTerminatingSemicolon(ctx: stellaParser.TerminatingSemicolonContext): T {
        return ctx.expr_.accept(this)
    }

    override fun visitParenthesisedExpr(ctx: stellaParser.ParenthesisedExprContext): T {
        return ctx.expr_.accept(this)
    }


    override fun visitTypeParens(ctx: stellaParser.TypeParensContext): T {
        return ctx.type_.accept(this)
    }

    override fun visitStart_Program(ctx: stellaParser.Start_ProgramContext): T {
        return ctx.program().accept(this)
    }

    override fun visitParenthesisedPattern(ctx: stellaParser.ParenthesisedPatternContext): T {
        return ctx.pattern_.accept(this)
    }

    override fun visitStart_Expr(ctx: stellaParser.Start_ExprContext): T {
        TODO("$action Start_Expr")
    }

    override fun visitPatternAsc(ctx: stellaParser.PatternAscContext): T {
        TODO("$action PatternAsc")
    }

    override fun visitStart_Type(ctx: stellaParser.Start_TypeContext): T {
        TODO("$action Start_Type")
    }

    override fun visitProgram(ctx: stellaParser.ProgramContext): T {
        TODO("$action Program")
    }

    override fun visitLanguageCore(ctx: stellaParser.LanguageCoreContext): T {
        TODO("$action LanguageCore")
    }

    override fun visitAnExtension(ctx: stellaParser.AnExtensionContext): T {
        TODO("$action AnExtension")
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext): T {
        TODO("$action DeclFun")
    }

    override fun visitDeclFunGeneric(ctx: stellaParser.DeclFunGenericContext): T {
        TODO("$action DeclFunGeneric")
    }

    override fun visitDeclTypeAlias(ctx: stellaParser.DeclTypeAliasContext): T {
        TODO("$action DeclTypeAlias")
    }

    override fun visitDeclExceptionType(ctx: stellaParser.DeclExceptionTypeContext): T {
        TODO("$action DeclExceptionType")
    }

    override fun visitDeclExceptionVariant(ctx: stellaParser.DeclExceptionVariantContext): T {
        TODO("$action DeclExceptionVariant")
    }

    override fun visitInlineAnnotation(ctx: stellaParser.InlineAnnotationContext): T {
        TODO("$action InlineAnnotation")
    }

    override fun visitParamDecl(ctx: stellaParser.ParamDeclContext): T {
        TODO("$action ParamDecl")
    }

    override fun visitFold(ctx: stellaParser.FoldContext): T {
        TODO("$action Fold")
    }

    override fun visitAdd(ctx: stellaParser.AddContext): T {
        TODO("$action Add")
    }

    override fun visitIsZero(ctx: stellaParser.IsZeroContext): T {
        TODO("$action IsZero")
    }

    override fun visitVar(ctx: stellaParser.VarContext): T {
        TODO("$action Var")
    }

    override fun visitTypeAbstraction(ctx: stellaParser.TypeAbstractionContext): T {
        TODO("$action TypeAbstraction")
    }

    override fun visitDivide(ctx: stellaParser.DivideContext): T {
        TODO("$action Divide")
    }

    override fun visitLessThan(ctx: stellaParser.LessThanContext): T {
        TODO("$action LessThan")
    }

    override fun visitDotRecord(ctx: stellaParser.DotRecordContext): T {
        TODO("$action DotRecord")
    }

    override fun visitGreaterThan(ctx: stellaParser.GreaterThanContext): T {
        TODO("$action GreaterThan")
    }

    override fun visitEqual(ctx: stellaParser.EqualContext): T {
        TODO("$action Equal")
    }

    override fun visitThrow(ctx: stellaParser.ThrowContext): T {
        TODO("$action Throw")
    }

    override fun visitMultiply(ctx: stellaParser.MultiplyContext): T {
        TODO("$action Multiply")
    }

    override fun visitConstMemory(ctx: stellaParser.ConstMemoryContext): T {
        TODO("$action ConstMemory")
    }

    override fun visitList(ctx: stellaParser.ListContext): T {
        TODO("$action List")
    }

    override fun visitTryCatch(ctx: stellaParser.TryCatchContext): T {
        TODO("$action TryCatch")
    }

    override fun visitHead(ctx: stellaParser.HeadContext): T {
        TODO("$action Head")
    }


    override fun visitNotEqual(ctx: stellaParser.NotEqualContext): T {
        TODO("$action NotEqual")
    }

    override fun visitConstUnit(ctx: stellaParser.ConstUnitContext): T {
        TODO("$action ConstUnit")
    }

    override fun visitSequence(ctx: stellaParser.SequenceContext): T {
        TODO("$action Sequence")
    }

    override fun visitConstFalse(ctx: stellaParser.ConstFalseContext): T {
        TODO("$action ConstFalse")
    }

    override fun visitAbstraction(ctx: stellaParser.AbstractionContext): T {
        TODO("$action Abstraction")
    }

    override fun visitConstInt(ctx: stellaParser.ConstIntContext): T {
        TODO("$action ConstInt")
    }

    override fun visitVariant(ctx: stellaParser.VariantContext): T {
        TODO("$action Variant")
    }

    override fun visitConstTrue(ctx: stellaParser.ConstTrueContext): T {
        TODO("$action ConstTrue")
    }

    override fun visitSubtract(ctx: stellaParser.SubtractContext): T {
        TODO("$action Subtract")
    }

    override fun visitTypeCast(ctx: stellaParser.TypeCastContext): T {
        TODO("$action TypeCast")
    }

    override fun visitIf(ctx: stellaParser.IfContext): T {
        TODO("$action If")
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext): T {
        TODO("$action Application")
    }

    override fun visitDeref(ctx: stellaParser.DerefContext): T {
        TODO("$action Deref")
    }

    override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext): T {
        TODO("$action IsEmpty")
    }

    override fun visitPanic(ctx: stellaParser.PanicContext): T {
        TODO("$action Panic")
    }

    override fun visitLessThanOrEqual(ctx: stellaParser.LessThanOrEqualContext): T {
        TODO("$action LessThanOrEqual")
    }

    override fun visitSucc(ctx: stellaParser.SuccContext): T {
        TODO("$action Succ")
    }

    override fun visitInl(ctx: stellaParser.InlContext): T {
        TODO("$action Inl")
    }

    override fun visitGreaterThanOrEqual(ctx: stellaParser.GreaterThanOrEqualContext): T {
        TODO("$action GreaterThanOrEqual")
    }

    override fun visitInr(ctx: stellaParser.InrContext): T {
        TODO("$action Inr")
    }

    override fun visitMatch(ctx: stellaParser.MatchContext): T {
        TODO("$action Match")
    }

    override fun visitLogicNot(ctx: stellaParser.LogicNotContext): T {
        TODO("$action LogicNot")
    }


    override fun visitTail(ctx: stellaParser.TailContext): T {
        TODO("$action Tail")
    }

    override fun visitRecord(ctx: stellaParser.RecordContext): T {
        TODO("$action Record")
    }

    override fun visitLogicAnd(ctx: stellaParser.LogicAndContext): T {
        TODO("$action LogicAnd")
    }

    override fun visitTypeApplication(ctx: stellaParser.TypeApplicationContext): T {
        TODO("$action TypeApplication")
    }

    override fun visitLetRec(ctx: stellaParser.LetRecContext): T {
        TODO("$action LetRec")
    }

    override fun visitLogicOr(ctx: stellaParser.LogicOrContext): T {
        TODO("$action LogicOr")
    }

    override fun visitTryWith(ctx: stellaParser.TryWithContext): T {
        TODO("$action TryWith")
    }

    override fun visitPred(ctx: stellaParser.PredContext): T {
        TODO("$action Pred")
    }

    override fun visitTypeAsc(ctx: stellaParser.TypeAscContext): T {
        TODO("$action TypeAsc")
    }

    override fun visitNatRec(ctx: stellaParser.NatRecContext): T {
        TODO("$action NatRec")
    }

    override fun visitUnfold(ctx: stellaParser.UnfoldContext): T {
        TODO("$action Unfold")
    }

    override fun visitRef(ctx: stellaParser.RefContext): T {
        TODO("$action Ref")
    }

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext): T {
        TODO("$action DotTuple")
    }

    override fun visitFix(ctx: stellaParser.FixContext): T {
        TODO("$action Fix")
    }

    override fun visitLet(ctx: stellaParser.LetContext): T {
        TODO("$action Let")
    }

    override fun visitAssign(ctx: stellaParser.AssignContext): T {
        TODO("$action Assign")
    }

    override fun visitTuple(ctx: stellaParser.TupleContext): T {
        TODO("$action Tuple")
    }

    override fun visitConsList(ctx: stellaParser.ConsListContext): T {
        TODO("$action ConsList")
    }

    override fun visitPatternBinding(ctx: stellaParser.PatternBindingContext): T {
        TODO("$action PatternBinding")
    }

    override fun visitBinding(ctx: stellaParser.BindingContext): T {
        TODO("$action Binding")
    }

    override fun visitMatchCase(ctx: stellaParser.MatchCaseContext): T {
        TODO("$action MatchCase")
    }

    override fun visitPatternVariant(ctx: stellaParser.PatternVariantContext): T {
        TODO("$action PatternVariant")
    }

    override fun visitPatternInl(ctx: stellaParser.PatternInlContext): T {
        TODO("$action PatternInl")
    }

    override fun visitPatternInr(ctx: stellaParser.PatternInrContext): T {
        TODO("$action PatternInr")
    }

    override fun visitPatternTuple(ctx: stellaParser.PatternTupleContext): T {
        TODO("$action PatternTuple")
    }

    override fun visitPatternRecord(ctx: stellaParser.PatternRecordContext): T {
        TODO("$action PatternRecord")
    }

    override fun visitPatternList(ctx: stellaParser.PatternListContext): T {
        TODO("$action PatternList")
    }

    override fun visitPatternCons(ctx: stellaParser.PatternConsContext): T {
        TODO("$action PatternCons")
    }

    override fun visitPatternFalse(ctx: stellaParser.PatternFalseContext): T {
        TODO("$action PatternFalse")
    }

    override fun visitPatternTrue(ctx: stellaParser.PatternTrueContext): T {
        TODO("$action PatternTrue")
    }

    override fun visitPatternUnit(ctx: stellaParser.PatternUnitContext): T {
        TODO("$action PatternUnit")
    }

    override fun visitPatternInt(ctx: stellaParser.PatternIntContext): T {
        TODO("$action PatternInt")
    }

    override fun visitPatternSucc(ctx: stellaParser.PatternSuccContext): T {
        TODO("$action PatternSucc")
    }

    override fun visitPatternVar(ctx: stellaParser.PatternVarContext): T {
        TODO("$action PatternVar")
    }



    override fun visitLabelledPattern(ctx: stellaParser.LabelledPatternContext): T {
        TODO("$action LabelledPattern")
    }

    override fun visitTypeTuple(ctx: stellaParser.TypeTupleContext): T {
        TODO("$action TypeTuple")
    }

    override fun visitTypeTop(ctx: stellaParser.TypeTopContext): T {
        TODO("$action TypeTop")
    }

    override fun visitTypeBool(ctx: stellaParser.TypeBoolContext): T {
        TODO("$action TypeBool")
    }

    override fun visitTypeRef(ctx: stellaParser.TypeRefContext): T {
        TODO("$action TypeRef")
    }

    override fun visitTypeRec(ctx: stellaParser.TypeRecContext): T {
        TODO("$action TypeRec")
    }

    override fun visitTypeSum(ctx: stellaParser.TypeSumContext): T {
        TODO("$action TypeSum")
    }

    override fun visitTypeVar(ctx: stellaParser.TypeVarContext): T {
        TODO("$action TypeVar")
    }

    override fun visitTypeVariant(ctx: stellaParser.TypeVariantContext): T {
        TODO("$action TypeVariant")
    }

    override fun visitTypeUnit(ctx: stellaParser.TypeUnitContext): T {
        TODO("$action TypeUnit")
    }

    override fun visitTypeNat(ctx: stellaParser.TypeNatContext): T {
        TODO("$action TypeNat")
    }

    override fun visitTypeBottom(ctx: stellaParser.TypeBottomContext): T {
        TODO("$action TypeBottom")
    }

    override fun visitTypeFun(ctx: stellaParser.TypeFunContext): T {
        TODO("$action TypeFun")
    }

    override fun visitTypeForAll(ctx: stellaParser.TypeForAllContext): T {
        TODO("$action TypeForAll")
    }

    override fun visitTypeRecord(ctx: stellaParser.TypeRecordContext): T {
        TODO("$action TypeRecord")
    }

    override fun visitTypeList(ctx: stellaParser.TypeListContext): T {
        TODO("$action TypeList")
    }

    override fun visitRecordFieldType(ctx: stellaParser.RecordFieldTypeContext): T {
        TODO("$action RecordFieldType")
    }

    override fun visitVariantFieldType(ctx: stellaParser.VariantFieldTypeContext): T {
        TODO("$action VariantFieldType")
    }
}