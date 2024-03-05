package org.old.typecheck

import org.antlr.v4.runtime.ParserRuleContext
import org.old.grammar.stellaParser
import org.old.grammar.stellaParser.PatternConsContext
import org.old.grammar.stellaParser.PatternContext
import org.old.grammar.stellaParser.PatternIntContext
import org.old.grammar.stellaParser.PatternListContext
import org.old.grammar.stellaParser.PatternRecordContext
import org.old.grammar.stellaParser.PatternSuccContext
import org.old.grammar.stellaParser.PatternUnitContext

fun PatternContext.isVarPattern(): Boolean {
    return this is stellaParser.PatternVarContext
}

fun checkExhaustivePatternsForNat(
    ctx: ParserRuleContext,
    patterns: List<PatternContext>
) {
    data class SuccPattern(val level: Int, val restMatched: Boolean)

    fun convertPattern(pat: PatternSuccContext): SuccPattern {
        var succ: PatternContext = pat
        var level = 0
        while (succ is PatternSuccContext) {
            level++
            succ = succ.pattern_
        }
        return if (succ is PatternIntContext)
            SuccPattern(level + succ.n.text.toInt(), false)
        else
            SuccPattern(level, true)
    }

    val succPatterns = patterns.mapNotNull { if (it is PatternSuccContext) convertPattern(it) else null }
    if (!succPatterns.any { it.restMatched })
        throw NonExhaustiveMatchPatterns(ctx)
    val minRestMatched = succPatterns.filter { it.restMatched }.minBy { it.level }.level
    val matchedNumbers = succPatterns.filter { !it.restMatched }.map { it.level }.toMutableSet()
    matchedNumbers.addAll(patterns.filterIsInstance<PatternIntContext>().map { it.n.text.toInt() })
    for (i in 0..<minRestMatched)
        if (i !in matchedNumbers)
            throw NonExhaustiveMatchPatterns(ctx)
}

fun checkExhaustivePatternsForList(
    ctx: ParserRuleContext,
    exprType: ListType,
    patterns: List<PatternContext>
) {
   data class ListPattern(val elemsPatterns: List<PatternContext>, val tailMatched: Boolean)

    fun convertPattern(pat: PatternListContext): ListPattern {
        return ListPattern(pat.patterns, false)
    }

    fun convertPattern(pat: PatternConsContext): ListPattern {
        var cons: PatternContext = pat
        val elemsPatterns = ArrayList<PatternContext>()
        while (cons is PatternConsContext) {
            elemsPatterns.add(cons.head)
            cons = cons.tail
        }
        if (cons.isVarPattern())
            return ListPattern(elemsPatterns, true)
        else {
            elemsPatterns.add(cons)
            return ListPattern(elemsPatterns, false)

        }
    }

    val listPatterns = patterns.mapNotNull {
        when (it) {
            is PatternListContext -> convertPattern(it)
            is PatternConsContext -> convertPattern(it)
            else -> null
        }
    }

    if (!listPatterns.any { it.elemsPatterns.isEmpty() })
        throw NonExhaustiveMatchPatterns(ctx)

    val minSizeWithTailMatched =
        listPatterns.filter { it.tailMatched }.minBy { it.elemsPatterns.size }.elemsPatterns.size
    for (listSize in 0..minSizeWithTailMatched) {
        val patsWithThatSize = listPatterns.filter { it.elemsPatterns.size == listSize }
        for(i in 0..<listSize){
            val elemPats = patsWithThatSize.map { it.elemsPatterns[i] }
            checkExhaustivePatterns(ctx, exprType.elementsType, elemPats)
        }
    }
}
fun expandPattern(pat: PatternContext): PatternContext{
    if(pat is stellaParser.PatternAscContext)
        return expandPattern(pat.pattern_)
    if(pat is stellaParser.ParenthesisedPatternContext)
        return expandPattern(pat.pattern_)
    return pat
}
fun checkExhaustivePatterns(
    ctx: ParserRuleContext,
    exprType: Type,
    patternContexts: List<PatternContext>
) {
    val patterns = patternContexts.map { expandPattern(it) }
    if (patterns.any { it.isVarPattern() })
        return
    when (exprType) {
        BoolType -> {
            if (!patterns.any { it is stellaParser.PatternTrueContext } || !patterns.any { it is stellaParser.PatternFalseContext })
                throw NonExhaustiveMatchPatterns(ctx)
        }

        is FunType -> throw NonExhaustiveMatchPatterns(ctx)
        is ListType -> {
            checkExhaustivePatternsForList(ctx, exprType, patterns)
        }

        NatType -> {
            checkExhaustivePatternsForNat(ctx, patterns)
        }

        is RecordType -> {
            val recPats = patterns.filterIsInstance<PatternRecordContext>()
            for (label in exprType.labels) {
                val labelPats = recPats.map { recPat -> recPat.patterns.first { it.label.text == label }.pattern_ }
                checkExhaustivePatterns(ctx, exprType.fieldsTypes.getValue(label), labelPats)
            }
        }

        is SumType -> {
            val inlPats = patterns.filterIsInstance<stellaParser.PatternInlContext>()
            val inrPats = patterns.filterIsInstance<stellaParser.PatternInrContext>()
            if (inlPats.isEmpty() || inrPats.isEmpty())
                throw NonExhaustiveMatchPatterns(ctx)
            checkExhaustivePatterns(ctx, exprType.inl, inlPats.map { it.pattern_ })
            checkExhaustivePatterns(ctx, exprType.inr, inrPats.map { it.pattern_ })
        }

        is TupleType -> {
            val tuplePats = patterns.filterIsInstance<stellaParser.PatternTupleContext>()
            for ((i, fieldType) in exprType.fieldsTypes.withIndex()) {
                checkExhaustivePatterns(ctx, fieldType, tuplePats.map { it.patterns[i] })
            }
        }

        UnitType -> {
            if (!patterns.any { it is PatternUnitContext })
                throw NonExhaustiveMatchPatterns(ctx)
        }

        is VariantType -> {
            val variantPats = patterns.filterIsInstance<stellaParser.PatternVariantContext>()
            for ((label, type) in exprType.variantsTypes.entries) {
                val labelPatterns = variantPats.filter { it.label.text == label }.map { it.pattern_ }
                if (type != null)
                    checkExhaustivePatterns(ctx, type, labelPatterns)
            }
        }
    }
}
