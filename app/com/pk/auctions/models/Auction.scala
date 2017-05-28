package com.pk.auctions.models

import java.util.UUID

import play.api.libs.json.Json


case class Auction(auctionId:UUID,
                   ownerId: UUID,
                   startingPrice: BigDecimal,
                   title: String,
                   status: String,
                   bids: List[Bid])

object Auction {

  implicit val format = Json.format[Auction]

}
