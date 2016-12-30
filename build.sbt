lazy val baseName  = "ScalaSwingTree"
lazy val baseNameL = "scala-swing-tree"

lazy val projectVersion = "0.1.2"
lazy val mimaVersion    = "0.1.0"

name               := baseName
organization       := "de.sciss"
moduleName         := baseNameL
version            := projectVersion
scalaVersion       := "2.11.8"
crossScalaVersions := Seq("2.12.1", "2.11.8", "2.10.6")
licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))
homepage           := Some(url(s"https://github.com/Sciss/${name.value}"))
description        := "A Scala Swing wrapper for the JTree component"

mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion)

// ---- main dependencies ----

lazy val swingPlusVersion = "0.2.2"

// ---- test dependencies ----

lazy val xmlVersion       = "1.0.6"

libraryDependencies += "de.sciss" %% "swingplus" % swingPlusVersion

libraryDependencies ++= {
  val sv = scalaVersion.value
  val is210 = sv.startsWith("2.10")
  if (is210) Nil else {
    val xml = "org.scala-lang.modules" %% "scala-xml" % xmlVersion % "test"
    xml :: Nil
  }
}

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture", "-encoding", "utf8", "-Xlint")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

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

