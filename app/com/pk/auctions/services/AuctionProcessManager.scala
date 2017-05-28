package com.pk.auctions.services

import com.pk.auctions.actors.AuctionProtocol.{AuctionCreated, AuctionFinished, BidAccepted}
import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.inmemory.query.scaladsl.InMemoryReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.google.inject.Inject
import com.google.inject.name.Named
import com.pk.auctions.models.{Auction, Bid}
import models.Bid
import com.pk.auctions.repos.AuctionRepo
import com.pk.auctions.services.AuctionProcessManager.StartJournalStreaming

import scala.concurrent.Future


object AuctionProcessManager {
  case object StartJournalStreaming

  def props(auctionRepo: AuctionRepo) = Props(new AuctionProcessManager(auctionRepo))
}

@Named("ProcessManager")
class AuctionProcessManager @Inject() (auctionRepo: AuctionRepo) extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()

  self ! StartJournalStreaming

  override def receive: Receive = {
    case StartJournalStreaming =>

      // docelowo tu powinien być użyty resumable projection pattern, żeby nie czytać eventów z journala za każdym restartem
      // ale jako że i tak cała persystencja jest in-memory to na chwilę obecną nam to nie przeszkadza
      // http://doc.akka.io/docs/akka/current/scala/persistence-query.html#resumable-projections
      val inMemoryJournal: InMemoryReadJournal =
        PersistenceQuery(context.system).readJournalFor[InMemoryReadJournal]("inmemory-read-journal")

      inMemoryJournal
        .eventsByTag("all", 0L)
        .mapAsync(1) { evt =>
          evt.event match {
            case msg: AuctionCreated =>
              log.info("Read auction created event")
              auctionRepo.upsertAuction(Auction(msg.auctionId, msg.ownerId, msg.startingPrice, msg.title, "inProgress", Nil))

            case msg: BidAccepted =>
              log.info("Read bid accepted event")
              auctionRepo.upsertBid(msg.auctionId, Bid(msg.userId, msg.offeredPrice, msg.timestamp))

            case msg: AuctionFinished =>
              log.info("Read auction finished event")
              auctionRepo.updateStatus(msg.auctionId, "finished")

            case msg =>
              log.info(s"unrecognized msg $msg")
              Future.successful(Done)
          }
        }
      .runWith(Sink.ignore)
  }
}
