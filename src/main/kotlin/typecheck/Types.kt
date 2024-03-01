package org.old.typecheck

sealed interface Type

data object NatType : Type {
    override fun toString(): String = "Nat"
}

data object BoolType : Type {
    override fun toString(): String = "Bool"

}

data object UnitType : Type {
    override fun toString(): String = "Unit"

}

data class TupleType(val fieldsTypes: List<Type>) : Type {
    override fun toString(): String = "{${fieldsTypes.joinToString(", ")}}"

}

data class RecordType(val fields: List<String>, val fieldsTypes: Map<String, Type>) : Type {

    override fun toString(): String = "{${fieldsTypes.entries.joinToString(", ") { "${it.key} : ${it.value}" }}}"
}

data class ListType(val elementsType: Type) : Type {
    override fun toString(): String = "[$elementsType]"

}

data class FunType(val paramsTypes: List<Type>, val retType: Type) : Type{
    override fun toString(): String = "fn (${paramsTypes.joinToString(", ")}) -> $retType"
}

data class VariantType(val fields: List<String>, val variantsTypes: Map<String, Type?>) : Type { // TODO we need preserve order of fields
    override fun toString(): String = "<|${variantsTypes.entries.joinToString(", ") { "${it.key} : ${it.value}" }}|>"
}

data class SumType(val inl: Type, val inr: Type) : Type {
    override fun toString(): String = "$inl + $inr"
}


fun isTypesEqual(type: Type, to: Type): Boolean {
    return type == to
}