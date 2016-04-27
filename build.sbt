import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val main = Project("explicit-implicits", file("."))
  .enablePlugins(GitVersioning)
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
    git.useGitDescribe := true,
    updateOptions := updateOptions.value.withCachedResolution(true),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(SpacesAroundMultiImports, false)
      .setPreference(DoubleIndentClassDeclaration, true)
  )
  .settings(
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
  )
  .settings(
    publishArtifact in Test := false,
    homepage := Some(url("https://github.com/lustefaniak/explicit-implicits")),
    description := "Bring your all implicits as single explicit",
    pomExtra :=
      <scm>
        <url>git@github.com:lustefaniak/explicit-implicits.git</url>
        <connection>scm:git:git@github.com:lustefaniak/explicit-implicits.git</connection>
      </scm>
        <developers>
          <developer>
            <id>lustefaniak</id>
            <url>https://github.com/lustefaniak</url>
          </developer>
        </developers>,
    bintrayCredentialsFile := file(".bintray_credentials"),
    bintrayVcsUrl := Some("https://github.com/lustefaniak/explicit-implicits.git"),
    bintrayRepository := "maven"
  )

addCommandAlias("formatAll", ";scalariformFormat;test:scalariformFormat")