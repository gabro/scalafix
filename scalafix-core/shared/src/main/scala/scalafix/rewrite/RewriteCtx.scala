package scalafix
package rewrite
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.syntax._
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.internal.util.SymbolOps.BottomSymbol
import scalafix.patch.PatchOps
import scalafix.patch.TokenPatch
import scalafix.patch.TokenPatch.Add
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch._
import scalafix.util.MatchingParens
import scalafix.util.TokenList
import org.scalameta.FileLine
import org.scalameta.logger

/** Bundle of useful things when implementing [[Rewrite]]. */
case class RewriteCtx(tree: Tree, config: ScalafixConfig) extends PatchOps {
  ctx =>
  def syntax: String =
    s"""${tree.input.syntax}
       |${logger.revealWhitespace(tree.syntax.take(100))}""".stripMargin
  override def toString: String = syntax
  def toks(t: Tree): Tokens = t.tokens(config.dialect)
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = new TokenList(tokens)
  lazy val matching: MatchingParens = MatchingParens(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  lazy val input: Input = tokens.head.input
  val reporter: ScalafixReporter = config.reporter

  // Debug utilities
  def mirror(implicit mirror: Mirror): Mirror =
    Database(mirror.database.entries.filter(_.input == input))
  def debugMirror()(implicit mirror: Mirror, fileLine: FileLine): Unit = {
    val db = this.mirror(mirror)
    debug(sourcecode.Text(db, "mirror"))
  }
  def debug(values: sourcecode.Text[Any]*)(implicit fileLine: FileLine): Unit = {
    // alias for org.scalameta.logger.
    logger.elem(values: _*)
  }

  // Syntactic patch ops.
  def removeImportee(importee: Importee): Patch =
    TreePatch.RemoveImportee(importee)
  def replaceToken(token: Token, toReplace: String): Patch =
    Add(token, "", toReplace, keepTok = false)
  def removeTokens(tokens: Tokens): Patch =
    tokens.foldLeft(Patch.empty)(_ + TokenPatch.Remove(_))
  def removeToken(token: Token): Patch = Add(token, "", "", keepTok = false)
  def replaceTree(from: Tree, to: String): Patch = {
    val tokens = toks(from)
    removeTokens(tokens) + tokens.headOption.map(x => addRight(x, to))
  }
  def rename(from: Name, to: Name): Patch =
    rename(from, to.value)
  def rename(from: Name, to: String): Patch =
    if (from.value == to) Patch.empty
    else
      ctx
        .toks(from)
        .headOption
        .fold(Patch.empty)(tok => Add(tok, "", to, keepTok = false))
  def addRight(tok: Token, toAdd: String): Patch = Add(tok, "", toAdd)
  def addLeft(tok: Token, toAdd: String): Patch = Add(tok, toAdd, "")

  // Semantic patch ops.
  def removeGlobalImport(symbol: Symbol)(implicit mirror: Mirror): Patch =
    RemoveGlobalImport(symbol)
  def addGlobalImport(symbol: Symbol)(implicit mirror: Mirror): Patch =
    TreePatch.AddGlobalSymbol(symbol)
  def addGlobalImport(importer: Importer)(implicit mirror: Mirror): Patch =
    AddGlobalImport(importer)
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit mirror: Mirror): Patch =
    TreePatch.ReplaceSymbol(fromSymbol, toSymbol)
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit mirror: Mirror): Patch =
    TreePatch.ReplaceSymbol(
      fromSymbol,
      Symbol.Global(BottomSymbol(), Signature.Term(toName)))

}
