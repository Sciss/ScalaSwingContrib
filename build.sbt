name               := "ScalaSwingTree"

organization       := "de.sciss"

moduleName         := "scala-swing-tree"

version            := "0.1.1"

scalaVersion       := "2.11.0"

crossScalaVersions := Seq("2.11.0", "2.10.4")

licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

homepage           := Some(url("https://github.com/Sciss/" + name.value))

description        := "A Scala Swing wrapper for the JTree component"

libraryDependencies ++= {
  val sv = scalaVersion.value
  val is210 = sv.startsWith("2.10")
  val swing = if (is210) {
    "org.scala-lang"         %  "scala-swing" % sv
  } else {
    "org.scala-lang.modules" %% "scala-swing" % "1.0.1"
  }
  val sq0 = swing :: Nil
  //
  if (is210) sq0 else {
    val xml = "org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test"
    xml :: sq0
  }
}

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (version.value endsWith "-SNAPSHOT")
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

