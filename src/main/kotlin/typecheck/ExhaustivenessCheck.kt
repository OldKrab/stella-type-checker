package org.old.typecheck

import org.old.grammar.stellaParser.*


private class NonExhaustive : Exception()

abstract class Ctor(open val size: Int) // Constructor
data object TrueCtor : Ctor(0)
data object FalseCtor : Ctor(0)
data object NilCtor : Ctor(0)
data object ConsCtor : Ctor(2)
data object ZeroCtor : Ctor(0)
data object SuccCtor : Ctor(1)
data class RecordCtor(override val size: Int, val labels: Set<String>) : Ctor(size)
data class VariantCtor(val nullary: Boolean, val label: String, val type: VariantType) : Ctor(if (nullary) 0 else 1)
data class TupleCtor(override val size: Int) : Ctor(size)
data object InlCtor : Ctor(1)
data object InrCtor : Ctor(1)
data object UnitCtor : Ctor(0)

sealed interface Pattern

data class CtorPattern(val ctor: Ctor, val pats: List<Pattern> = emptyList()) : Pattern
data object Wildcard : Pattern

typealias PatternRow = List<Pattern>
typealias PatternMatrix = List<PatternRow>

private fun getListPattern(pat: PatternListContext, type: ListType): Pattern {
    var res = CtorPattern(NilCtor)
    for (elemPat in pat.patterns.asReversed()) {
        res = CtorPattern(ConsCtor, listOf(getPattern(elemPat, type.elementsType), res))
    }
    return res
}

private fun getConsPattern(pat: PatternConsContext, type: ListType): Pattern {
    return CtorPattern(ConsCtor, listOf(getPattern(pat.head, type.elementsType), getPattern(pat.tail, type)))
}

private fun getIntPattern(pat: PatternIntContext): Pattern {
    var res = CtorPattern(ZeroCtor)
    val value = pat.text.toInt()
    for (elemPat in value downTo 1) {
        res = CtorPattern(SuccCtor, listOf(res))
    }
    return res
}

private fun getRecordPattern(pat: PatternRecordContext, type: RecordType): Pattern {
    return CtorPattern(RecordCtor(pat.patterns.size, pat.patterns.map { it.label.text }.toSet()), pat.patterns
        .sortedBy { it.label.text }.map { getPattern(it.pattern_, type.fieldsTypes.getValue(it.label.text)) })
}

private fun getTuplePattern(pat: PatternTupleContext, type: TupleType): Pattern {
    val ctor = TupleCtor(pat.patterns.size)
    return CtorPattern(ctor, pat.patterns.zip(type.fieldsTypes).map { getPattern(it.first, it.second) })
}

private fun getVariantPattern(pat: PatternVariantContext, varType: VariantType): Pattern {
    val label = pat.label.text
    val elemType = varType.variantsTypes.getValue(label)
    val elemPat = if (elemType == null) emptyList() else listOf(getPattern(pat.pattern_, elemType))
    return CtorPattern(VariantCtor(elemType == null, label, varType), elemPat)
}

private fun getInlPattern(pat: PatternInlContext, varType: SumType): Pattern {
    val elemType = varType.inl
    val elemPat = listOf(getPattern(pat.pattern_, elemType))
    return CtorPattern(InlCtor, elemPat)
}

private fun getInrPattern(pat: PatternInrContext, varType: SumType): Pattern {
    val elemType = varType.inr
    val elemPat = listOf(getPattern(pat.pattern_, elemType))
    return CtorPattern(InrCtor, elemPat)
}


private fun getPattern(pat: PatternContext, type: Type): Pattern {
    return when (pat) {
        is PatternVarContext -> Wildcard
        is PatternTrueContext -> CtorPattern(TrueCtor)
        is PatternFalseContext -> CtorPattern(FalseCtor)
        is PatternListContext -> getListPattern(pat, type as ListType)
        is PatternIntContext -> getIntPattern(pat)
        is PatternConsContext -> getConsPattern(pat, type as ListType)
        is PatternSuccContext -> CtorPattern(SuccCtor, listOf(getPattern(pat.pattern_, type)))
        is PatternRecordContext -> getRecordPattern(pat, type as RecordType)
        is PatternInlContext -> getInlPattern(pat, type as SumType)
        is PatternInrContext -> getInrPattern(pat, type as SumType)
        is PatternTupleContext -> getTuplePattern(pat, type as TupleType)
        is PatternVariantContext -> getVariantPattern(pat, type as VariantType)
        is PatternUnitContext -> CtorPattern(UnitCtor)
        else -> throw NotImplementedError("For ${pat::class.simpleName}")
    }
}

private fun getPatternMatrix(patterns: List<PatternContext>, type: Type): PatternMatrix {
    return patterns.map { listOf(getPattern(it, type)) }
}

private fun specializeRow(p: PatternRow, ctor: Ctor): PatternRow? {
    val first = p[0]
    val rest = p.slice(1..<p.size)
    return when (first) {
        is CtorPattern -> {
            if (ctor == first.ctor)
                first.pats + rest
            else
                null
        }

        is Wildcard -> List(ctor.size) { Wildcard } + rest
    }
}

private fun getCtorsSet(ctor: Ctor): Set<Ctor> {
    if (ctor is VariantCtor) {
        return ctor.type.variantsTypes.map { VariantCtor(it.value == null, it.key, ctor.type) }.toSet()
    }
    if (ctor is RecordCtor || ctor is TupleCtor || ctor is UnitCtor) {
        return setOf(ctor)
    }

    val sets = setOf(
        setOf(TrueCtor, FalseCtor),
        setOf(NilCtor, ConsCtor),
        setOf(ZeroCtor, SuccCtor),
        setOf(InlCtor, InrCtor)
    )
    val ctorSets = sets.flatMap { set -> set.map { Pair(it, set) } }.associate { it }
    return ctorSets.getValue(ctor)
}

private fun isCompleteSet(ctors: Set<Ctor>): Boolean {
    if (ctors.isEmpty())
        return false
    return getCtorsSet(ctors.first()) == ctors
}


private fun defaultRow(p: PatternRow): PatternRow? {
    val first = p[0]
    return when (first) {
        is CtorPattern -> null
        Wildcard -> p.slice(1..<p.size)
    }
}


private fun checkExhaustive(matrix: PatternMatrix, q: PatternRow) {
    if (matrix.isEmpty())
        throw NonExhaustive()
    if (q.isEmpty())
        return


    when (val firstQ = q[0]) {
        is CtorPattern -> {
            val newMatrix = matrix.mapNotNull { specializeRow(it, firstQ.ctor) }
            checkExhaustive(newMatrix, specializeRow(q, firstQ.ctor)!!)
        }

        is Wildcard -> {
            val ctors = matrix.map { it[0] }.filterIsInstance<CtorPattern>().map { it.ctor }.toSet()
            if (isCompleteSet(ctors)) {
                for (ctor in ctors) {
                    val newMatrix = matrix.mapNotNull { specializeRow(it, ctor) }
                    checkExhaustive(newMatrix, specializeRow(q, ctor)!!)

                }
            } else {
                val newMatrix = matrix.mapNotNull { defaultRow(it) }
                checkExhaustive(newMatrix, q.slice(1..<q.size))
            }
        }
    }
}


private fun PatternContext.isVarPattern(): Boolean {
    return this is PatternVarContext
}


private fun expandPattern(pat: PatternContext): PatternContext {
    if (pat is PatternAscContext)
        return expandPattern(pat.pattern_)
    if (pat is ParenthesisedPatternContext)
        return expandPattern(pat.pattern_)
    return pat
}

fun checkExhaustivePatterns(
    exprType: Type,
    patternContexts: List<PatternContext>
): Boolean {

    val patterns = patternContexts.filter {it !is PatternCastAsContext}.map { expandPattern(it) }
    if (patterns.any { it.isVarPattern() })
        return true
    try {
        checkExhaustive(getPatternMatrix(patterns, exprType), listOf(Wildcard))
    } catch (e: NonExhaustive) {
        return false
    }
    return true
}
