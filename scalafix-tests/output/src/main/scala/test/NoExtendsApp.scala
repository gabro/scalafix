package test

object Main {
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

trait Something
object Main2 extends Something {
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

object Main3 extends Something {
  def main(args: Array[String]) = {
    println(s"Hello, ${args(0)}")
    println(s"Hello 2, ${args(0)}")
  }
}

