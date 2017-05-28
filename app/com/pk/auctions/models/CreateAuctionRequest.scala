package com.pk.auctions.models

import java.util.UUID

import play.api.libs.json.{Json, OFormat}


case class CreateAuctionRequest(startingPrice: BigDecimal, durationInSeconds: Int, title: String, auctionId: Option[UUID])

object CreateAuctionRequest {

  implicit val format: OFormat[CreateAuctionRequest] = Json.format[CreateAuctionRequest]

}