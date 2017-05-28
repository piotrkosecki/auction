package com.pk.auctions.models

import java.util.UUID

import play.api.libs.json.Json

case class Bid(userId: UUID, bidPrice: BigDecimal, timestamp: Long)

object Bid {

  implicit val format = Json.format[Bid]

}
