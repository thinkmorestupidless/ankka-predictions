package com.thinkmorestupidless.predictions.markets.components

import com.github.plokhotnyuk.jsoniter_scala.core.{readFromString, writeToString, JsonValueCodec}
import com.thinkmorestupidless.ankka.core.{CommandError, Codecs, ErrorCode}
import com.thinkmorestupidless.predictions.markets.Refusal

/**
 * A refusal as the error a handler answers with, and back, as the exchange's components do: the
 * message is the refusal as JSON, since ankka 0.14.2 carries a rejection's message and code between
 * components and not its details. Only a refusal's codes are read back.
 */
object Refused:

  private val Key = "refusal"

  private given JsonValueCodec[Refusal] = Codecs.make[Refusal]

  private val codes = Set(ErrorCode.BadRequest, ErrorCode.Conflict, ErrorCode.Forbidden)

  def error(refusal: Refusal): CommandError = error(refusal, code(refusal))

  def error(refusal: Refusal, code: ErrorCode): CommandError =
    CommandError(writeToString(refusal), code, refusal.names + (Key -> refusal.reason))

  def unapply(error: CommandError): Option[Refusal] =
    if !codes(error.code) || !error.message.startsWith("{") then None
    else
      try Some(readFromString[Refusal](error.message))
      catch case _: com.github.plokhotnyuk.jsoniter_scala.core.JsonReaderException => None

  private def code(refusal: Refusal): ErrorCode = refusal.reason match
    case "out of order" | "state" => ErrorCode.Conflict
    case _                        => ErrorCode.BadRequest
