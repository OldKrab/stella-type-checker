package org.old.typecheck

class TypeContext {
    val vars = HashMap<String, MutableList<Type>>()

    fun getVarType(name: String): Type? = vars[name]?.lastOrNull()

    fun addVariable(name: String, type: Type) {
        vars.computeIfAbsent(name) { ArrayList() }.add(type)
    }

    fun removeVariable(name: String) {
        vars[name]!!.removeLast()
    }
}