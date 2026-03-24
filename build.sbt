// To publish to Sonatype Central:
// sbt
//   ++ 2.12.19
//   publishSigned
//   sonatypeCentralUpload
//   exit
// rm -rf target/sonatype-staging/
// sbt
//   ++ 2.13.18
//   publishSigned
//   sonatypeCentralUpload

import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / scalaVersion := "2.13.18"
ThisBuild / crossScalaVersions := Seq("2.12.19", "2.13.18")

ThisBuild / organization := "com.github.werk"
ThisBuild / version := "0.1.6"

ThisBuild / organizationName := "werk"
ThisBuild / organizationHomepage := Some(url("https://github.com/werk"))
ThisBuild / description := "A type-safe router library for Scala and Scala.js"
ThisBuild / licenses := List("MIT" -> url("http://www.opensource.org/licenses/mit-license.php"))
ThisBuild / homepage := Some(url("https://github.com/werk/router4s"))

ThisBuild / scmInfo := Some(ScmInfo(
    url("https://github.com/werk/router4s"),
    "scm:git@github.com:werk/router4s.git"
))

ThisBuild / developers := List(Developer(
    id    = "werk",
    name  = "Michael Werk Ravnsmed",
    email = "",
    url   = url("https://github.com/werk")
))

ThisBuild / Test / publishArtifact := false

sonatypeCredentialHost := sonatypeCentralHost
ThisBuild / publishTo := sonatypePublishToBundle.value

lazy val root = project.in(file("."))
    .aggregate(router4sJS, router4sJVM)
    .settings(publish / skip := true)

lazy val router4s = crossProject(JSPlatform, JVMPlatform).in(file("."))
    .settings(
        name := "router4s"
    )

lazy val router4sJVM = router4s.jvm
lazy val router4sJS  = router4s.js