import sbt.*

/** Every external version, written once. */
object Dependencies {

  object V {
    val scala = "3.9.0"

    /**
     * The ankka the release builds against, and the one a consumer pins (spec 004, assumptions).
     * `-Dankka.version=<v>` overrides it, for a locally published one.
     */
    val ankka = sys.props.getOrElse("ankka.version", "0.14.2")

    val munit           = "1.3.6"
    val munitScalacheck = "1.2.0"
  }

  private def ankka(module: String) = "com.thinkmorestupidless" %% s"ankka-$module" % V.ankka

  val ankkaCore    = ankka("core")
  val ankkaSdk     = ankka("sdk")
  val ankkaTestkit = ankka("testkit")

  val munit           = "org.scalameta" %% "munit"            % V.munit
  val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % V.munitScalacheck
}
