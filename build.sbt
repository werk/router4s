
scalaVersion in ThisBuild := "2.12.1"

lazy val root = project.in(file(".")).
    aggregate(routerJS, routerJVM).
    settings(
        publish := {},
        publishM2 := {},
        publishLocal := {}
    )

lazy val router = crossProject.in(file(".")).
    settings(
        name := "router",
        organization := "com.github.werk",
        version := "0.1-SNAPSHOT"
    ).
    jvmSettings(
    ).
    jsSettings(
        libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )

lazy val routerJVM = router.jvm
lazy val routerJS = router.js
