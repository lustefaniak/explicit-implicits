

lazy val basicSettings = Seq(
  organization := "com.lustefaniak.explicitimplicits",
  scalaVersion := "2.11.8",
  scalacOptions += "-encoding",
  scalacOptions += "utf8",
  scalacOptions += "-feature",
  scalacOptions += "-unchecked",
  scalacOptions += "-deprecation",
  scalacOptions += "-language:_"
  //scalacOptions += "-Ymacro-debug-lite",

)


lazy val main = Project("main", file("."))
  .settings(basicSettings)
  .dependsOn(macroSub)

lazy val macroSub = Project("macro", file("macro"))
  .settings(basicSettings)
  .settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
  )
