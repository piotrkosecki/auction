package com.pk.auctions.services

import com.pk.auctions.actors.AuctionProtocol.{AuctionEvent, BidCommand, CreateAuctionCommand}
import com.pk.auctions.actors.CoordinatorActor.{BidAuction, CreateAuctionProtocol}
import akka.actor.ActorRef
import akka.pattern.ask
import com.google.inject.Inject
import com.google.inject.name.Named

import scala.concurrent.duration._
import scala.concurrent.Future


trait AuctionWriteService {
  def createAuction(createAuction: CreateAuctionProtocol): Future[AuctionEvent]

  def bid(bid: BidAuction): Future[AuctionEvent]
}

class AuctionWriteServiceImpl @Inject() (@Named("Coordinator") auctionActor: ActorRef) extends AuctionWriteService {
  implicit val timeout: akka.util.Timeout = 5 seconds

  override def createAuction(createAuction: CreateAuctionProtocol): Future[AuctionEvent] = {
    (auctionActor ? createAuction).mapTo[AuctionEvent]
  }

  override def bid(bid: BidAuction): Future[AuctionEvent] = {
    (auctionActor ? bid).mapTo[AuctionEvent]
  }
}
