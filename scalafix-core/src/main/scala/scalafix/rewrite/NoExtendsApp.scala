package scalafix
package rewrite

import scala.meta.{Symbol => _, _}
import scalafix.syntax._
import scalafix.util.{Whitespace => _, _}
import scala.meta.contrib._
import scala.meta.tokens.Token._
import scala.meta.semantic.Signature
import scala.collection.mutable
import org.scalameta.logger

case class NoExtendsApp(mirror: Mirror) extends SemanticRewrite(mirror) {

  override def rewrite(ctx: RewriteCtx): Patch = {
    def templateBodyStatements(template: Template): Tokens = {
      val tokens = template.tokens
      val result = for {
        close <- tokens.lastOption
        if close.is[RightBrace]
        open <- ctx.matching.open(close.asInstanceOf[RightBrace])
      } yield
        tokens
          .dropWhile(_.pos.start.offset <= open.pos.start.offset)
          .dropRight(1)
      result.get
    }

    def indent(tokens: Tokens, indentation: Tokens): Patch = {
      val lastNewLine = tokens.dropRightWhile(t => !t.is[Newline]).last
      tokens.collect {
        case nl @ Newline() if nl != lastNewLine =>
          ctx.addRight(nl, indentation.syntax)
      }.asPatch
    }

    def indentation(tokens: Tokens): Tokens = {
      val firstNonWhitespaceToken = tokens.dropWhile(_.is[Whitespace]).head
      tokens
        .dropRightWhile(_ != firstNonWhitespaceToken)
        .dropRight(1)
        .takeRightWhile(t => !t.is[Newline])
    }

    //TODO(gabro): this should take care or extends/with stuff
    def removeExtends(tree: Tree): Patch = {
      val nameToken = tree.tokens.head
      val extendsToken = ctx.tokenList
        .leading(nameToken)
        .find(t => t.is[KwExtends] || t.is[KwWith])
        .get
      ctx.tokenList
        .slice(ctx.tokenList.prev(extendsToken),
               ctx.tokenList.next(ctx.tokenList.next(nameToken)))
        .map(ctx.removeToken)
        .asPatch
    }

    ctx.tree.collect {
      case t: Defn.Object if t.templ.parents.exists {
            case c: Ctor.Ref =>
              c.symbolOpt.map(_.normalized.syntax) == Some("_root_.scala.App.")
          } && t.templ.stats.isDefined =>
        val body = templateBodyStatements(t.templ)
        val bodyIndentation = indentation(body)
        val open =
          ctx.addLeft(body.head,
                      s"\n${bodyIndentation}def main(args: Array[String]) = {")
        val close = ctx.addRight(body.last, s"${bodyIndentation}}\n")
        val appTree = t.templ.parents
          .collect { case c: Ctor.Ref => c }
          .find(
            _.symbolOpt.map(_.normalized.syntax) == Some("_root_.scala.App."))
          .get
        removeExtends(appTree) + open + indent(body, bodyIndentation) + close
    }.asPatch
  }
}
