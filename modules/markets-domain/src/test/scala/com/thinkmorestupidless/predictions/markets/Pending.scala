package com.thinkmorestupidless.predictions.markets

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/**
 * The scenarios of a feature file or directory that wait on a spec whose rules are not built yet,
 * by name, for a `GherkinSuite`'s `ranElsewhere`: each is reported ignored with the spec that lifts
 * it, never passed (spec 004 research V4a). A name that is no scenario fails the suite, so an entry
 * cannot outlive its scenario.
 */
object Pending:

  private val title = """^\s*Scenario(?: Outline)?:\s*(.+?)\s*$""".r

  /** Every scenario under `path`, each waiting on `spec`. */
  def all(path: String, spec: String): Map[String, String] =
    names(Paths.get(path)).map(_ -> spec).toMap

  private def names(path: Path): Vector[String] =
    val files =
      if Files.isDirectory(path) then
        val stream = Files.walk(path)
        try stream.iterator.asScala.filter(_.toString.endsWith(".feature")).toVector
        finally stream.close()
      else Vector(path)
    files.flatMap(f => Files.readAllLines(f).asScala.collect { case title(name) => name })
