lazy val baseName  = "ScalaSwingTree"
lazy val baseNameL = "scala-swing-tree"

lazy val projectVersion = "0.2.0"
lazy val mimaVersion    = "0.2.0"

lazy val commonSettings = Seq(
  name               := baseName,
  organization       := "de.sciss",
  moduleName         := baseNameL,
  version            := projectVersion,
  scalaVersion       := "2.12.8",
  crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0-RC1"),
  licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  homepage           := Some(url(s"https://git.iem.at/sciss/${name.value}")),
  description        := "A Scala Swing wrapper for the JTree component",
  mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
  libraryDependencies ++= Seq(
    "de.sciss"               %% "swingplus" % deps.main.swingPlus,
    "org.scala-lang.modules" %% "scala-xml" % deps.test.scalaXml % Test
  ),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint", "-Xsource:2.13")
) ++ publishSettings

lazy val deps = new {
  val main = new {
    val swingPlus = "0.4.2"
  }
  val test = new {
    val scalaXml  = "1.2.0"
  }
}

lazy val root = project.in(file("."))
  .settings(commonSettings)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
  <scm>
    <url>git@github.com:Sciss/{n}.git</url>
    <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
  </scm>
  <developers>
    <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
    </developer>
    <developer>
      <id>kenbot</id>
      <name>Ken Scambler</name>
      <url>http://github.com/kenbot</url>
    </developer>
  </developers>
  }
)

