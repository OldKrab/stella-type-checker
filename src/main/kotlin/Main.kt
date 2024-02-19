package org.old

fun antlrTest() {
    val source = """
        language core;
        extend with #multiparameter-functions ;

        fn twice(k : fn(Nat) -> Nat) -> fn(Nat) -> Nat {
          return fn(x : Nat) {
            return k(k(x))
          }
        }
        fn f(x : Bool, y : Nat) -> Bool { return x }

        fn main(n : Nat) -> Nat {
          return (if true then twice else twice)
            ( fn(x : Nat) { return succ(succ(x)) } )
            (0)
        }
    """.trimIndent()
    val parser = getParser(source)
    val program = parser.program()
    println(program.getPrettyString(parser))
}


fun main(args: Array<String>) {
    antlrTest()
    println("Hi, ${if (args.isEmpty()) "anon" else args[0]}")
}