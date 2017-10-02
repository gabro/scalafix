# Rules

Scalafix comes with a few rules out-of-the-box.
These rules have been chosen to meet the long-term goal of scalafix to
[clarify the Scala to Dotty migration path](http://scala-lang.org/blog/2016/05/30/scala-center-advisory-board.html#the-first-meeting).
To create custom rules, see @sect.ref{scalafix-core}.

  @sect{ExplicitResultTypes}
    This rule has been renamed into @sect.ref{ExplicitResultTypes}

  @sect(RemoveUnusedImports.toString)
    This rule acts upon "Unused import" warnings emitted by the Scala compiler.
    See @lnk("slick/slick/pulls#1736", "https://github.com/slick/slick/pull/1736")
    for an example diff from running @code{sbt "scalafix RemoveUnusedImports"}.

    @p
      To use this rule:
      @ul
        @li
          enable @code{-Ywarn-unused-import}
        @li
          disable @code{-Xfatal-warnings}. Unfortunately, the Scala compiler
          does not support finer grained control over the severity level
          per message kind. See @metaIssue(924) for a possible workaround
          in the near future.

    @hl.scala
      // before
      import scala.List
      import scala.collection.{immutable, mutable}
      object Foo { immutable.Seq.empty[Int] }

      // after
      import scala.collection.immutable
      object Foo { immutable.Seq.empty[Int] }

    @p
      @note. This rule does a best-effort at preserving original formatting.
      In some cases, the rewritten code may be formatted weirdly

      @hl.scala
        // before
        import scala.concurrent.{
          CancellationException,
          TimeoutException
        }
        // after
        import scala.concurrent.

          TimeoutException

      It's recommended to use a code formatter after running this rule.

  @sect(RemoveXmlLiterals.toString)
    This rules replaces XML literals with a @code{xml""} interpolator
    from @lnk("scala-xml-quote", "https://github.com/densh/scala-xml-quote")
    project.

    @hl.scala
      // tries to use single quote when possible
      <div>{bar}</div>
      xml"<div>${bar}</div>"

      // multi-line literals get triple quote
      <div>
        <span>{"Hello"}</span>
      </div>
      xml"""<div>
        <span>${"Hello"}</span>
      </div>"""

      // skips XML literals in pattern position
      x match { case <a></a> => }
      x match { case <a></a> => }

      // replaces escaped {{ with single curly brace {
      <div>{{</div>
      xml"<div>{</div>"


  @sect(ProcedureSyntax.toString)
    "Procedure syntax" is not supported in Dotty.
    Methods that use procedure syntax should use regular method syntax instead.
    For example,

    @hl.scala
      // before: procedure syntax
      def main(args: Seq[String]) {
        println("Hello world!")
      }
      // after: regular syntax
      def main(args: Seq[String]): Unit = {
        println("Hello world!")
      }

  @sect(DottyVolatileLazyVal.toString)
    @p
      Adds a @code{@@volatile} annotation to lazy vals.
      The @code{@@volatile} annotation is needed to maintain thread-safe
      behaviour of lazy vals in Dotty.

    @hl.scala
      // before
      lazy val x = ...
      // after
      @@volatile lazy val x = ...

    @p
      With @code{@@volatile}, Dotty uses a deadlock free scheme that is
      comparable-if-not-faster than the scheme used in scalac.

  @sect(ExplicitUnit.toString)
    @p
      Adds an explicit @code{Unit} return type to @code{def} declarations without a return type

    @hl.scala
      // before
      trait A {
        def doSomething
      }
      // after
      trait A {
        def doSomething: Unit
      }

    @p
      Such members already have a return type of @code{Unit} and sometimes this is unexpected.
      Adding an explicit return type makes it more obvious.

  @sect(DottyVarArgPattern.toString)
    @p
      Replaces @code{@@} symbols in VarArg patterns with a colon (@code{:}). See @lnk{http://dotty.epfl.ch/docs/reference/changed/vararg-patterns.html}

    @hl.scala
      // before
      case List(1, 2, xs @@ _*)
      // after
      case List(1, 2, xs : _*)

  @sect(NoAutoTupling.toString)
    @p
      Adds explicit tuples around argument lists where auto-tupling is occurring.

    @p
      To use this rule:
      @ul
        @li
          enable @code{-Ywarn-adapted-args} (note, -Yno-adapted-args will fail
          compilation, which prevents scalafix from running)
        @li
          disable @code{-Xfatal-warnings}. Unfortunately, the Scala compiler
          does not support finer grained control over the severity level
          per message kind. See @metaIssue(924) for a possible workaround
          in the near future.

    @hl.scala
      // before
      def someMethod(t: (Int, String)) = ...
      someMethod(1, "something")
      // after
      def someMethod(t: (Int, String)) = ...
      someMethod((1, "something"))

    @p
      Auto-tupling is a feature that can lead to unexpected results, making code to compile when one would expect a compiler error instead. Adding explicit tuples makes it more obvious.

    @note Some auto-tupling cases are left unfixed, namely the ones involving constructor application using `new`

      @hl.scala
        case class Foo(x: (String, Boolean))
        new Foo("string", true) // won't be fixed
        Foo("string", true)     // will be fixed

      This is a known limitation.

  @sect(NoValInForComprehension.toString)
    @p
      Removes @code{val} from definitions in for-comprehension.

    @hl.scala
      // before
      for {
        n <- List(1, 2, 3)
        val inc = n + 1
      } yield inc
      // after
      for {
        n <- List(1, 2, 3)
        inc = n + 1
      } yield inc

    @p
      The two syntaxes are equivalent and the presence of the @code{val} keyword has been deprecated since Scala 2.10.

  @sect(Sbt1.toString)
    @experimental
    @p
      To fix the sbt build sources of your build use @b{sbtfix}:
      @ul
        @li
          Install @sect.ref{semanticdb-sbt}
        @li
          Start a new sbt shell session or inside an active shell run @code{> reload}
        @li
          Run @code{> sbtfix Sbt1}
        @li
          Note that the command is @b{sbtfix} to run on your sbt build sources.
    @p
      To fix sources of an sbt 0.13 plugin use @b{scalafix}:
      @ul
        @li
          Install the @code{semanticdb-sbt} compiler plugin to your sbt 0.13 plugin:
          @hl.scala
            // build.sbt
            lazy val my210project = project.settings(
              scalaVersion := "2.10.6", // semanticdb-sbt only supports 2.10.6
              addCompilerPlugin(
                "org.scalameta" % "semanticdb-sbt" % "@V.semanticdbSbt" cross CrossVersion.full
              )
            )

        @li
          Run @code{my210project/scalafix Sbt1}
        @li
          Note that the command is @b{scalafix} to run on regular project sources.

    @hl.scala
      // before
      x <+= (y in Compile)
      // after
      x += (y in Compile).value


    @pureTable
      @def row(what: String)(status: Frag*) = tr(td(code(what)), td(status))
      @thead
        @tr
          @th
            Change
          @th
            Status
      @tbody
        @row{<+= to .value}
          Done.
        @row{<++= to .value}
          Done.
        @row{<<= to .value}
          Done.
        @row{extends Build}
          Not handled.
          See @lnk{http://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html#Migrating+from+the+Build+trait}
        @row{(task1, task2).map}
          Not handled.
          See @lnk{http://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html#Migrating+from+the+tuple+enrichments}


  @sect(NoInfer.toString)
    @since("0.5.0")

    This rule reports errors when the compiler inferred one of
    the following types
    @ul
      @NoInfer.badSymbolNames.map(li(_))

    Example:
    @hl.scala
      MyCode.scala:7: error: [NoInfer.any] Inferred Any
        List(1, "")
            ^
    @h3{Known limitations}

    @ul
      @li
        Scalafix does not yet expose an way to disable rules across
        regions of code, track @issue(241) for updates.
        bt1



  @sect(Disable.toString)
    @since("0.5.0")
    This rule reports errors when a "disallowed" symbol is referenced.

    Example:
    @hl.scala
      MyCode.scala:7: error: [DisallowSymbol.asInstanceOf] asInstanceOf is disabled.
        myValue.asInstanceOf[String]
                ^
    @h3{Configuration}

    @p
      By default, this rule does allows all symbols.
      To disallow a symbol,

    @hl.scala
      Disable.symbols = [
        "scala.Option.get"
        "scala.Any.asInstanceOf"
      ]

  @sect{Planned rules...}
    See @lnk("here", "https://github.com/scalacenter/scalafix/labels/rule").
