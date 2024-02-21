package org.old.typecheck

sealed interface Type

data object NatType : Type
data object BoolType : Type
data object UnitType : Type
data class TupleType(val fieldsTypes: List<Type>) : Type
data class RecordType(val fieldsTypes: Map<String, Type>) : Type
data class ListType(val elementsType: Type) : Type
data class FunType(val paramsTypes: List<Type>, val retType: Type): Type


fun isAssignable(type: Type, to: Type): Boolean {
    return type == to
}