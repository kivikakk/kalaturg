import scala.sys.process._

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

lazy val cxxsim = inputKey[Unit]("Elaborate, build cxxsim, and run it")
cxxsim := {
  (Compile / run).evaluated
  if (("make cxxsim" !) != 0) {
    throw new IllegalStateException("cxxsim failed to build")
  }
  if (("build/cxxsim --vcd" !) != 0) {
    throw new IllegalStateException("cxxsim run failed")
  }
}
