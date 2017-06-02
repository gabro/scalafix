/* ONLY
rewrites = [NoExtendsApp]
*/
package test

object Main extends App {
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

trait Something
object Main2 extends Something with App {
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

object Main3 extends App with Something {
  println(s"Hello, ${args(0)}")
  println(s"Hello 2, ${args(0)}")
}

