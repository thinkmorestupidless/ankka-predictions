package com.thinkmorestupidless.predictions.exchange.components

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString, writeToString, JsonValueCodec}
import com.thinkmorestupidless.ankka.core.{CommandError, Codecs, ErrorCode}
import com.thinkmorestupidless.predictions.exchange.Refusal

/**
 * A domain refusal as the error a handler answers with, and back. The message is the refusal as
 * JSON (`{"reason":…,"names":{…}}`), because ankka 0.14.2 carries a rejection's message and code
 * between components and not its details; the names are the details too, for a caller on the same
 * node. Only a refusal's codes are read back, so a fault is never taken for one. A refusal of the
 * state, the order or an id already used is a conflict; every other refusal is a bad request.
 */
object Refused:

  private val Key = "refusal"

  private given JsonValueCodec[Refusal] = Codecs.make[Refusal]

  private val codes = Set(ErrorCode.BadRequest, ErrorCode.Conflict, ErrorCode.Forbidden)

  def error(refusal: Refusal): CommandError =
    CommandError(writeToString(refusal), code(refusal), refusal.names + (Key -> refusal.reason))

  def unapply(error: CommandError): Option[Refusal] =
    if !codes(error.code) || !error.message.startsWith("{") then None
    else
      try Some(readFromString[Refusal](error.message))
      catch case _: com.github.plokhotnyuk.jsoniter_scala.core.JsonReaderException => None

  private def code(refusal: Refusal): ErrorCode = refusal.reason match
    case "out of order" | "state" | "id" | "key" => ErrorCode.Conflict
    case _                                       => ErrorCode.BadRequest
