package scalafix.sbt

import scala.language.reflectiveCalls

import scala.meta.scalahost.sbt.ScalahostSbtPlugin
import scalafix.Versions
import sbt.File
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import scalafix.internal.sbt.ScalafixCompletions
import scalafix.internal.sbt.ScalafixJarFetcher
import sbt.Def

object ScalafixPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  object autoImport {
    val scalafix: InputKey[Unit] = inputKey[Unit]("Run scalafix rewrite.")
    val scalafixVerbose: SettingKey[Boolean] =
      settingKey[Boolean]("pass --verbose to scalafix")
    val scalafixVersion: SettingKey[String] = settingKey[String](
      s"Which scalafix version to run. Default is ${Versions.version}.")
    val scalafixScalaVersion: SettingKey[String] = settingKey[String](
      s"Which scala version to run scalafix from. Default is ${Versions.scala212}.")
    val scalafixConfig: SettingKey[Option[File]] =
      settingKey[Option[File]](
        ".scalafix.conf file to specify which scalafix rules should run.")
  }
  import ScalahostSbtPlugin.autoImport._
  import scalafix.internal.sbt.CliWrapperPlugin.autoImport._
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile),
    cliWrapperMainClass := "scalafix.cli.Cli$",
    scalafixVerbose := false,
    scalafixVersion := Versions.version,
    scalafixScalaVersion := Versions.scala212,
    cliWrapperClasspath := {
      val jars = ScalafixJarFetcher.fetchJars(
        "ch.epfl.scala",
        s"scalafix-cli_${scalafixScalaVersion.value}",
        scalafixVersion.value
      )
      if (jars.isEmpty) {
        throw new MessageOnlyException("Unable to download scalafix-cli jars!")
      }
      jars
    }
  )

  // hack to avoid illegal dynamic reference, can't figure out how to use inputTaskDyn.
  private val workingDirectory = file(sys.props("user.dir"))
  private val scalafixParser = ScalafixCompletions.parser(workingDirectory)

  lazy val scalafixSettings = Seq(
    scalafix.in(Compile) := scalafixTaskImpl(Compile).evaluated,
    scalafix.in(Test) := scalafixTaskImpl(Test).evaluated,
    scalafix := scalafixTaskImpl(Compile, Test).evaluated
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    scalafixSettings

  def scalafixTaskImpl(
      config: Configuration*): Def.Initialize[InputTask[Unit]] =
    Def.inputTaskDyn(
      scalafixTaskImpl(
        scalafixParser.parsed,
        ScopeFilter(configurations = inConfigurations(config: _*))))
  def scalafixTaskImpl(
      inputArgs: Seq[String],
      filter: ScopeFilter): Def.Initialize[Task[Unit]] =
    Def.task {
      val verbose = if (scalafixVerbose.value) "--verbose" :: Nil else Nil
      val main = cliWrapperMain.value
      val log = streams.value.log

      compile.all(filter).value // trigger compilation
      val classpath = classDirectory.all(filter).value.asPath
      val directoriesToFix: Seq[String] =
        unmanagedSourceDirectories.all(filter).value.flatten.collect {
          case p if p.exists() => p.getAbsolutePath
        }
      val baseArgs = Set[String](
        "--project-id",
        name.value,
        "--no-sys-exit",
        "--non-interactive"
      )
      val args: Seq[String] = {
        // run scalafix rewrites
        val config =
          scalafixConfig.value
            .map(x => "--config" :: x.getAbsolutePath :: Nil)
            .getOrElse(Nil)
        val rewriteArgs =
          if (inputArgs.nonEmpty)
            inputArgs.flatMap("-r" :: _ :: Nil)
          else Nil
        val sourceroot: String =
          scalametaSourceroot.??(workingDirectory).value.getAbsolutePath
        // only fix unmanaged sources, skip code generated files.
        verbose ++
          config ++
          rewriteArgs ++
          baseArgs ++
          List(
            "--sourceroot",
            sourceroot,
            "--classpath",
            classpath
          )
      }
      if (classpath.nonEmpty) {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 11 | 12)) if directoriesToFix.nonEmpty =>
            val nonBaseArgs = args.filterNot(baseArgs).mkString(" ")
            log.info(s"Running scalafix $nonBaseArgs")
            main.main((args ++ directoriesToFix).toArray)
          case _ => // do nothing
        }
      }
    }

  private[scalafix] implicit class XtensionFormatClasspath(paths: Seq[File]) {
    def asPath: String =
      paths.toIterator
        .collect { case f if f.exists() => f.getAbsolutePath }
        .mkString(java.io.File.pathSeparator)
  }
}
