package org.specs2
import control.Exceptions._
import scala.io.Source

object Specs2Variables {

  lazy val version = versionLine.flatMap(extractVersion).getOrElse("version not found")
  lazy val isSnapshot = version endsWith "SNAPSHOT"

  lazy val branch = if (isSnapshot) "master" else version

  lazy val guideOfficialDir = "guide/"
  lazy val guideSnapshotDir = guideOfficialDir + "-SNAPSHOT/guide/"
  lazy val guideDir         = (if (isSnapshot) guideSnapshotDir else guideOfficialDir)

  lazy val apiOfficialDir = "http://etorreborre.github.com/specs2/api/" + version + "/"
  lazy val apiSnapshotDir = "http://etorreborre.github.com/specs2/api/latest/"
  lazy val apiDir         = (if (isSnapshot) apiSnapshotDir else apiOfficialDir)

  private lazy val versionLine = buildSbt.flatMap(_.getLines.find(line => line contains "version"))
  private def extractVersion(line: String) = "version\\s*\\:\\=\\s*\"(.*)\"".r.findFirstMatchIn(line).map(_.group(1))
  private lazy val buildSbt = tryo(Source.fromFile("build.sbt"))((e:Exception) => println("can't find the build.sbt file "+e.getMessage))

  implicit def toVersionedText(t: String): VersionedText = VersionedText(t)
  case class VersionedText(t: String) {
    /**
     * set the version and branch tags in the pages
     */
    def replaceVariables = {
      Seq("VERSION"        -> version,
          "BRANCH"         -> branch,
          "API"            -> apiDir,
          "API_OFFICIAL"   -> apiOfficialDir,
          "API_SNAPSHOT"   -> apiSnapshotDir,
          "GUIDE"          -> guideDir,
          "GUIDE_OFFICIAL" -> guideOfficialDir,
          "GUIDE_SNAPSHOT" -> guideSnapshotDir).foldLeft(t) { case (res, (k, v)) => res.replaceAll("\\$\\{SPECS2_"+k+"\\}", v) }
    }
  }

}