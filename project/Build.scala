import sbt._
import Keys._

import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {

  val appName = "bark"
  val appVersion = "0.1"

  override lazy val settings = super.settings ++
    Seq(
      name := "bark",
      version := "0.1",
      scalaVersion := "2.10.0",
      parallelExecution in Test := false,
      resolvers ++= Seq(Resolver.mavenLocal,
        "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
        "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
        "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      ensimeConfig := sexp(
        key(":compiler-args"), sexp("-Ywarn-dead-code", "-Ywarn-shadowing"),
        key(":formatting-prefs"), sexp(
          key(":alignParameters"), true,
          key(":AlignSingleLineCaseStatements"), true,
          key(":CompactControlReadability"), true,
          key(":CompactStringConcatenation"), true,
          key(":DoubleIndentClassDeclaration"), true,
          key(":IndentLocalDefs"), true,
          key(":IndentPackageBlocks"), true,
          key(":IndentSpaces"), 2,
          key(":MultilineScaladocCommentsStartOnFirstLine"), true,
          key(":PreserveSpaceBeforeArguments"), false,
          key(":PreserveDanglingCloseParenthesis"), false,
          key(":RewriteArrowSymbols"), true,
          key(":SpaceBeforeColon"), false,
          key(":SpaceInsideBrackets"), false,
          key("SpacesWithinPatternBinders"), true
        )
      )
    )

  val appDependencies = Seq(
    "org.scalaz" %% "scalaz-core" % "7.0.0-M7" withSources(),
    "org.scalaz" %% "scalaz-effect" % "7.0.0-M7" withSources(),
    "com.google.protobuf" % "protobuf-java" % "2.4.1" withSources(),
    "org.specs2" %% "specs2" % "1.13",
    "io.spray" %%  "spray-json" % "1.2.3",
    "com.typesafe.akka" % "akka-actor_2.10" % "2.2-20130410-001403",

    "com.google.protobuf" % "protobuf-java" % "2.4.1" withSources(),
    "play" %% "play-iteratees" % "2.1-RC2" withSources(),
    "com.chuusai" %% "shapeless" % "1.2.4" withSources(),

    "nl.gideondk" %% "sentinel" % "0.2.1" withSources()

  )

  lazy val root = Project(id = "bark",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      libraryDependencies ++= appDependencies,
      mainClass := Some("Main")
    ) ++ spray.boilerplate.BoilerplatePlugin.Boilerplate.settings ++ Format.settings
  )
}

object Format {

  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences
  )

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, true).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(IndentLocalDefs, true).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, false).
      setPreference(RewriteArrowSymbols, true).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

