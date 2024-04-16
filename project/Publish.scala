import sbt._
import Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._

object Publish {

  val SuppressJavaDocsAndSources = Seq(
    doc / sources := Seq(),
    packageDoc / publishArtifact := false,
    packageSrc / publishArtifact := false
  )

  val ReleaseToSonatype = Seq(
      credentials ++= Seq(
          Credentials(
            "Sonatype Nexus Repository Manager",
            "oss.sonatype.org",
            sys.env.getOrElse("USERNAME", ""),
            sys.env.getOrElse("PASSWORD", "")
          ),
          Credentials(
            "GnuPG Key ID",
            "gpg",
            "E9F32B46ABCE86181ABDBF8ECE902ED363A2FA58", // key identifier
            "ignored"                                   // this field is ignored; passwords are supplied by pinentry
          )
        ),
    sonatypeProfileName := "nl.pragmasoft",
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/jacum/akka-sensors")),
    scmInfo := Some(ScmInfo(browseUrl = url("https://github.com/jacum/akka-sensors"), connection = "scm:git@github.com:jacum/akka-sensors.git")),
    pomExtra := (
          <developers>
        <developer>
          <id>PragmaSoft</id>
          <name>PragmaSoft</name>
        </developer>
      </developers>
        ),
    publishMavenStyle := true,
    publishTo := sonatypePublishToBundle.value,
    Test / publishArtifact := false,
    packageDoc / publishArtifact := true,
    packageSrc / publishArtifact := true,
    pomIncludeRepository := (_ => false),
    releaseCrossBuild := true,
    releaseIgnoreUntrackedFiles := true,
    releaseProcess := Seq[ReleaseStep](
          checkSnapshotDependencies,
          inquireVersions,
          runClean,
//      runTest, // can't run test w/cross-version release
          releaseStepCommandAndRemaining("+publishSigned"),
          releaseStepCommand("sonatypeBundleRelease")
        )
  )

  val settings =
    if (sys.env.contains("USERNAME")) {
      println(s"Releasing to Sonatype as ${sys.env("USERNAME")}")
      ReleaseToSonatype
    } else SuppressJavaDocsAndSources

}
