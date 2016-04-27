import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

lazy val main = Project("explicit-implicits", file("."))
  .disablePlugins(SbtScalariform)
  .settings(SbtScalariform.defaultScalariformSettings)
  .settings(organization := "com.lustefaniak.explicitimplicits")
  .settings(
    scalaVersion := "2.11.8",
    scalacOptions += "-encoding",
    scalacOptions += "utf8",
    scalacOptions += "-feature",
    scalacOptions += "-unchecked",
    scalacOptions += "-deprecation",
    scalacOptions += "-language:_",
    //scalacOptions += "-Ymacro-debug-lite",
    updateOptions := updateOptions.value.withCachedResolution(true),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(SpacesAroundMultiImports, false)
      .setPreference(DoubleIndentClassDeclaration, true))
  .settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
  )

addCommandAlias("formatAll", ";scalariformFormat;test:scalariformFormat")