package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.testkit.GherkinSuite
import com.thinkmorestupidless.predictions.exchange.*

/**
 * `features/libraries/compatibility.feature` for the exchange journal (research R12): every fixture
 * of every release under `journal/` replays through this build to the value its release recorded.
 * The feature's releases are its own example; here the earlier release is each fixture directory
 * (0.0.0 before the first tag) and the later one is this build.
 */
class JournalCompatibilitySuite extends GherkinSuite("features/libraries/compatibility.feature"):

  private var journals: Vector[(String, Vector[Journal.Line])]       = Vector.empty
  private var replayed: Vector[(String, Journal.Replayed[Exchange])] = Vector.empty

  override def beforeEach(context: BeforeEach): Unit =
    journals = Vector.empty
    replayed = Vector.empty

  private def fixtures(name: String): Vector[(String, Vector[Journal.Line])] =
    Journal.fixtures.collect {
      case (release, file) if file.getFileName.toString == name =>
        s"$release/$name" -> Journal.read(file)
    }

  private def replay(lines: Vector[Journal.Line]): Journal.Replayed[Exchange] =
    Journal.replay[Exchange, ExchangeEvent](lines, Exchange.empty, Exchange.replay)

  Given("a journal of a book written by the release {string}") { (_: String) =>
    journals =
      Journal.fixtures.map((release, file) => s"$release/${file.getFileName}" -> Journal.read(file))
    assert(journals.nonEmpty, s"no fixture under ${Journal.root}")
  }

  Given("a journal with an event the release {string} no longer produces") { (_: String) =>
    // Every case is in it, and this build's domain produces few of them until specs 005 and 006.
    journals = fixtures("every-event.jsonl")
    assert(journals.nonEmpty, "no every-event fixture")
  }

  Given("a journal with an event with the manifest {string}") { (manifest: String) =>
    journals = fixtures("opened.jsonl").map { (name, lines) =>
      val (events, state) = lines.partition(_.manifest != "state")
      name -> ((events.take(1) :+ Journal.Line(manifest, "{}")) ++ events.drop(
        1
      ) ++ events ++ state)
    }
    assert(journals.nonEmpty, "no opened fixture")
  }

  When("the journal is replayed by the release {string}") { (_: String) =>
    replayed = journals.map((name, lines) => name -> replay(lines))
  }

  Then("the rebuilt book equals the book the release {string} rebuilds") { (_: String) =>
    replayed.foreach { (name, r) =>
      assertEquals(r.failure, None, name)
      assertEquals(Some(r.value), r.recorded, name)
    }
  }

  Then("the event is applied as the release {string} applied it") { (_: String) =>
    replayed.foreach { (name, r) =>
      assertEquals(r.failure, None, name)
      assertEquals(Some(r.value), r.recorded, name)
    }
  }

  Then("the replay fails naming the manifest {string}") { (manifest: String) =>
    replayed.foreach { (name, r) =>
      assert(r.failure.exists(_.contains(s"'$manifest'")), s"$name: ${r.failure}")
    }
  }

  Then("no event after it is applied") { () =>
    // The unknown manifest is the second line: only the first event was applied.
    replayed.foreach((name, r) => assertEquals(r.applied, 1, name))
  }

  test(
    "an event whose type is unknown fails the replay naming the type, with nothing after it applied"
  ) {
    fixtures("opened.jsonl").foreach { (name, lines) =>
      val (events, _) = lines.partition(_.manifest != "state")
      val manifest    = events.head.manifest
      val unknown = Journal.Line(manifest, """{"type":"Teleported","at":"2026-10-31T10:00:00Z"}""")
      val r       = replay((events.take(1) :+ unknown) ++ events.drop(1))
      assert(r.failure.exists(_.contains("'Teleported'")), s"$name: ${r.failure}")
      assertEquals(r.applied, 1, name)
    }
  }
