package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.Serializer

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/**
 * A journal fixture (research R12): one `manifest<TAB>json` line per event, as a release's
 * serializers wrote it, and a last `state<TAB>json` line with the value the events produced.
 */
object Journal:

  final case class Line(manifest: String, json: String)

  /** Where the fixtures live: one directory per release that wrote them. */
  val root: Path = Path.of("modules/markets-components/src/test/resources/journal")

  def write[S, E](file: Path, events: Vector[E], state: S)(using
      event: Serializer[E],
      value: Serializer[S]
  ): Unit =
    val lines = events.map(e => s"${event.manifest}\t${String(event.toBytes(e), UTF_8)}") :+
      s"state\t${String(value.toBytes(state), UTF_8)}"
    Files.createDirectories(file.getParent)
    Files.write(file, lines.asJava): Unit

  def read(file: Path): Vector[Line] =
    Files.readAllLines(file).asScala.toVector.filter(_.nonEmpty).map { line =>
      val tab = line.indexOf('\t')
      require(tab > 0, s"$file: a line with no manifest: $line")
      Line(line.substring(0, tab), line.substring(tab + 1))
    }

  /** Every fixture, by its release and name. */
  def fixtures: Vector[(String, Path)] =
    def listed(dir: Path): Vector[Path] =
      val stream = Files.list(dir)
      try stream.iterator.asScala.toVector.sorted
      finally stream.close()
    listed(root)
      .filter(Files.isDirectory(_))
      .flatMap(release =>
        listed(release).filter(_.toString.endsWith(".jsonl")).map(release.getFileName.toString -> _)
      )

  /** What a replay came to: the value, the events applied, and the failure that stopped it. */
  final case class Replayed[S](value: S, applied: Int, failure: Option[String], recorded: Option[S])

  /**
   * The events replayed through this build's serializer and `replay`, in order, stopping at the
   * first line whose manifest is not the event's or whose JSON does not read, with why.
   */
  def replay[S, E](lines: Vector[Line], empty: S, step: (S, E) => S)(using
      event: Serializer[E],
      value: Serializer[S]
  ): Replayed[S] =
    val (events, state) = lines.partition(_.manifest != "state")
    val recorded        = state.lastOption.map(l => value.fromBytes(l.json.getBytes(UTF_8)))
    @scala.annotation.tailrec
    def go(at: S, rest: Vector[Line], applied: Int): Replayed[S] =
      if rest.isEmpty then Replayed(at, applied, None, recorded)
      else
        val line = rest.head
        val more = rest.tail
        if line.manifest != event.manifest then
          Replayed(at, applied, Some(s"unknown manifest '${line.manifest}'"), recorded)
        else
          val decoded =
            try Right(event.fromBytes(line.json.getBytes(UTF_8)))
            catch
              case e: Exception =>
                Left(s"cannot read the type '${typeOf(line.json)}': ${e.getMessage}")
          decoded match
            case Right(e)  => go(step(at, e), more, applied + 1)
            case Left(why) => Replayed(at, applied, Some(why), recorded)
    go(empty, events, 0)

  private val typeField = "\"type\"\\s*:\\s*\"([^\"]*)\"".r

  private def typeOf(json: String): String =
    typeField.findFirstMatchIn(json).fold("none")(_.group(1))
