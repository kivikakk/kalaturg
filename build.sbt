ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "0.1.0"
ThisBuild / organization := "ee.kivikakk"

val chiselVersion = "6.3.0"

lazy val root = (project in file("."))
  .settings(
    name := "kalaturg",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel"     % chiselVersion,
      "org.scalatest"     %% "scalatest"  % "3.2.18" % "test",
      "edu.berkeley.cs"   %% "chiseltest" % "6.0.0",
      "ee.hrzn"           %% "chryse"     % "0.1.0-SNAPSHOT",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls", "-deprecation", "-feature", "-Xcheckinit",
      "-Ymacro-annotations",
    ),
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full,
    ),
  )
