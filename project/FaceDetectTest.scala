import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object FaceDetectTestBuild extends Build {
  def scalaSettings = Seq(
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq(
      "-optimize",
      "-unchecked",
      "-deprecation"
    ),
    javaOptions ++= Seq("-Dlog4j.debug=true")
  )

  def extraAssemblySettings = Seq(
    mergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
      case "reference.conf"                                    => MergeStrategy.concat
      case _	=> MergeStrategy.first
      }
    )
  def buildSettings = Project.defaultSettings ++
    scalaSettings ++
    assemblySettings ++
    extraAssemblySettings ++
    Seq(
      libraryDependencies ++= Seq(
        "com.livestream" %% "scredis" % "2.0.0"
      )
    )

  lazy val root = {
    val settings = buildSettings ++ Seq(name := "FaceDetectTest")
    Project(id = "FaceDetectTest", base = file("."), settings = settings)
  }
}
