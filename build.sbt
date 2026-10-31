// ankka-predictions: the markets and exchange libraries eitheror's predictions module composes.
//
// Four modules (spec 004, research R1). The two domains depend on `ankka-core` alone, so "a rule
// reads no clock, wallet or network" is a fact of the build and the pom, not a discipline; the two
// components modules hold the entities, the workflow, the views and the timers written against
// them, with the outside world behind ports the consumer supplies. A `v*` tag publishes the four to
// Maven Central; the version is sbt-dynver's from the tag.

import Dependencies.*

ThisBuild / scalaVersion  := V.scala
ThisBuild / organization  := "com.thinkmorestupidless"
ThisBuild / versionScheme := Some("early-semver")

ThisBuild / homepage := Some(url("https://github.com/thinkmorestupidless/ankka-predictions"))
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer(
    "thinkmorestupidless",
    "Trevor Burton-McCreadie",
    "",
    url("https://github.com/thinkmorestupidless")
  )
)

// Suites that start containers contend when they overlap. Do not undo it.
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

lazy val common = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature", "-Wunused:all", "-Werror", "-source:3.7"),
  javacOptions ++= Seq("--release", "21"),
  Test / fork              := true,
  Test / parallelExecution := false,
  // The living features are at the repository's root, and a forked test JVM starts where this says.
  Test / baseDirectory := (ThisBuild / baseDirectory).value,
  libraryDependencies ++= Seq(munit % Test, ankkaTestkit % Test)
)

lazy val exchangeDomain = project
  .in(file("modules/exchange-domain"))
  .settings(common)
  .settings(
    name := "ankka-predictions-exchange-domain",
    libraryDependencies ++= Seq(ankkaCore, munitScalacheck % Test),
    // The property suite's count (spec 005 SC-002), passed to the forked test JVM.
    Test / javaOptions ++= sys.props
      .get("predictions.sequences")
      .map(n => s"-Dpredictions.sequences=$n")
      .toSeq
  )

lazy val marketsDomain = project
  .in(file("modules/markets-domain"))
  .settings(common)
  .settings(
    name := "ankka-predictions-markets-domain",
    libraryDependencies ++= Seq(ankkaCore, munitScalacheck % Test)
  )

lazy val exchangeComponents = project
  .in(file("modules/exchange-components"))
  // The suites are the example consumer (research R11): it builds both libraries.
  .dependsOn(exchangeDomain % "compile->compile;test->test", marketsComponents % Test)
  .settings(common)
  .settings(
    name := "ankka-predictions-exchange-components",
    libraryDependencies ++= Seq(ankkaSdk)
  )

lazy val marketsComponents = project
  .in(file("modules/markets-components"))
  .dependsOn(marketsDomain % "compile->compile;test->test")
  .settings(common)
  .settings(
    name := "ankka-predictions-markets-components",
    libraryDependencies ++= Seq(ankkaSdk)
  )

lazy val root = project
  .in(file("."))
  .aggregate(exchangeDomain, marketsDomain, exchangeComponents, marketsComponents)
  .settings(name := "ankka-predictions", publish / skip := true)
