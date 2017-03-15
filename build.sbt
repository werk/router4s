enablePlugins(ScalaJSPlugin)

name := "router4s"
organization := "com.github.werk"
version := "0.1.0-SNAPSHOT"

// Publish cross versions with: sbt +publish
//crossScalaVersions := Seq("2.11.8", scalaVersion.value)
scalaVersion := "2.12.1"
scalacOptions += "-feature"


publishMavenStyle := true
publishArtifact in Test := false
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if(isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
    else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra :=
    <url>https://github.com/werk/router4s</url>
        <licenses>
            <license>
                <name>MIT-style</name>
                <url>http://www.opensource.org/licenses/mit-license.php</url>
                <distribution>repo</distribution>
            </license>
        </licenses>
        <scm>
            <url>git@github.com:werk/router4s.git</url>
            <connection>scm:git:git@github.com:werk/router4s.git</connection>
        </scm>
        <developers>
            <developer>
                <id>werk</id>
                <name>Michael Werk Ravnsmed</name>
                <url>https://github.com/werk</url>
            </developer>
        </developers>


lazy val root = project.in(file(".")).
    aggregate(router4sJS, router4sJVM)

lazy val router4s = crossProject.in(file(".")).
    jvmSettings(
    ).
    jsSettings(
        libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )

lazy val router4sJVM = router4s.jvm
lazy val router4sJS = router4s.js
