package com.pk.auctions.actors

import java.util.UUID

import com.pk.auctions.actors.AuctionProtocol.{BidCommand, BidRejected, CreateAuctionCommand}
import com.pk.auctions.actors.CoordinatorActor.{BidAuction, CreateAuctionProtocol}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import com.google.inject.Singleton
import com.google.inject.name.Named

import scala.concurrent.duration._

// jakbyśmy dodali normalną bazę to chielibyśmy tutaj mieć peristent actora, jednak dla celów PoC nie jest to konieczne
// docelowo pewnie zrobiłbym zamiast tego ClusterSharding

object CoordinatorActor {

  sealed trait CoordinatorProtocol
  case class CreateAuctionProtocol(auctionId: UUID, startingPrice: BigDecimal, durationInSeconds: Int, ownerId: UUID, title: String)
  case class BidAuction(auctionId: UUID, offeredPrice: BigDecimal, userId: UUID)

  def props = Props[CoordinatorActor]

}

@Singleton
@Named("Coordinator")
class CoordinatorActor extends Actor with ActorLogging {

  import context._

  var auctionMap: Map[UUID, ActorRef] = Map.empty

  implicit val timeout: Timeout = 5 seconds

  override def receive: Receive = {

    case msg: CreateAuctionProtocol =>
      val auction = auctionMap.get(msg.auctionId) match {
        case Some(actorRef) => actorRef
        case None =>
          val actorRef = system.actorOf(AuctionActor.props(msg.auctionId))
          auctionMap += msg.auctionId -> actorRef
          actorRef
      }
      (auction ? CreateAuctionCommand(msg.auctionId, msg.startingPrice, msg.durationInSeconds, msg.ownerId, msg.title)).pipeTo(sender())

    case msg: BidAuction =>
      auctionMap.get(msg.auctionId) match {
        case Some(actorRef) => (actorRef ? BidCommand(msg.offeredPrice, msg.userId)).pipeTo(sender())
        case None => sender() ! BidRejected("auction does not exist")
      }

    case msg =>
      log.warning(s"Unhandled message $msg")
  }
}
