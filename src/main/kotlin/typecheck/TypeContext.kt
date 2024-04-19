package org.old.typecheck

class TypeContext {
    private val vars = HashMap<String, MutableList<Type>>()
     var exceptionsType: Type? = null

    fun getVarType(name: String): Type? = vars[name]?.lastOrNull()

    fun addVariable(name: String, type: Type) {
        vars.computeIfAbsent(name) { ArrayList() }.add(type)
    }

    fun removeVariable(name: String) {
        vars[name]!!.removeLast()
    }

    fun removeAllVariables(names: Iterable<String>) {
        for (name in names)
            vars[name]!!.removeLast()
    }
}