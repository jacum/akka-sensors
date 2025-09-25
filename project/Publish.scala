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
            "s01.oss.sonatype.org",
            sys.env.getOrElse("SONATYPE_USERNAME", ""),
            sys.env.getOrElse("SONATYPE_PASSWORD", "")
          ),
          Credentials(
            "GnuPG Key ID",
            "gpg",
            "80639E9F764EA1049652FDBBDA743228BD43ED35", // key identifier
            "ignored"                                   // this field is ignored; passwords are supplied by pinentry
          )
        ),
    sonatypeProfileName := "nl.pragmasoft.sensors",
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
          setReleaseVersion,
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
