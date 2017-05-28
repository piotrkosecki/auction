package com.pk.auctions.adapters

import com.pk.auctions.actors.AuctionProtocol.{AuctionCreated, AuctionFinished, BidAccepted}
import akka.persistence.journal.{Tagged, WriteEventAdapter}


class EventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  def withTag(event: Any, tag: String) = Tagged(event, Set(tag))

  override def toJournal(event: Any): Any = event match {
    case _: AuctionCreated =>
      withTag(event, "all")
    case _: BidAccepted =>
      withTag(event, "all")
    case _: AuctionFinished =>
      withTag(event, "all")
    case _ => event
  }
}