package com.pk.auctions.models

import java.util.UUID

import play.api.libs.json.Json


case class BidAuctionRequest(userId: UUID, price: BigDecimal)

object BidAuctionRequest {

  implicit val format = Json.format[BidAuctionRequest]

}