
scalaVersion in ThisBuild := "2.12.1"

lazy val root = project.in(file(".")).
    aggregate(router4sJS, router4sJVM).
    settings(
        publish := {},
        publishM2 := {},
        publishLocal := {}
    )

lazy val router4s = crossProject.in(file(".")).
    settings(
        name := "router4s",
        organization := "com.github.werk",
        version := "0.1-SNAPSHOT"
    ).
    jvmSettings(
    ).
    jsSettings(
        libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )

lazy val router4sJVM = router4s.jvm
lazy val router4sJS = router4s.js
