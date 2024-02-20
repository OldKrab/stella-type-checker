import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.old.getParser
import org.old.getPrettyString

class ParserTest{
    @Test
    fun `Parse without errors`(){
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
        val (parser, errListener, program) = getParser(source)

        assertEquals(0, errListener.getSyntaxErrors().size)
        println(program.getPrettyString(parser))
    }

    @Test
    fun `Parse with errors`(){
        val source = """
        language core;
        extend with #multiparameter-functions ;

        fun `Parse with errors`(){
            val source = "la la"
        }
        """.trimIndent()
        val (_, errListener, _) = getParser(source)
        assertTrue(errListener.getSyntaxErrors().isNotEmpty())
        errListener.getSyntaxErrors().forEach { println(it) }
    }
}