import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences.AlignSingleLineCaseStatements
import scalariform.formatter.preferences.DoubleIndentClassDeclaration
import scalariform.formatter.preferences.SpacesAroundMultiImports

lazy val basicSettings:Seq[sbt.Setting[_]] = SbtScalariform.defaultScalariformSettings ++ Seq(
  organization := "com.lustefaniak.explicitimplicits",
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
    .setPreference(DoubleIndentClassDeclaration, true)
)


lazy val main = Project("main", file("."))
  .disablePlugins(SbtScalariform)
  .settings(basicSettings)
  .dependsOn(macroSub)
  .aggregate(macroSub)

lazy val macroSub = Project("macro", file("macro"))
  .disablePlugins(SbtScalariform)
  .settings(basicSettings)
  .settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
  )

addCommandAlias("formatAll", ";scalariformFormat;test:scalariformFormat")