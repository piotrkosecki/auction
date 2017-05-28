package com.pk.auctions.repos

import java.util.UUID

import akka.Done
import com.pk.auctions.models.{Auction, Bid}
import models.Bid
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}


trait AuctionRepo {

  def list: Future[List[Auction]]

  def upsertAuction(auction: Auction): Future[Done]

  def updateStatus(auctionId: UUID, status: String): Future[Done]

  def get(auctionId: UUID): Future[Option[Auction]]

  def upsertBid(auctionId: UUID, bid: Bid): Future[Done]

}

class SlickAuctionRepo(val driver: JdbcProfile, val database: Database)(implicit ec: ExecutionContext) extends AuctionRepo {

  import driver.api._

  val auctionTableName = "auctions"

  case class AuctionRecord(auctionId: UUID, ownerId: UUID, startingPrice: BigDecimal, title: String, status: String)

  protected class AuctionTable(tag: Tag) extends Table[AuctionRecord](tag, auctionTableName) {

    def auctionId = column[UUID]("auction_id", O.PrimaryKey)

    def ownerId = column[UUID]("owner_id")

    def startingPrice = column[BigDecimal]("starting_price")

    def title = column[String]("title")

    def status = column[String]("status")

    override def * : ProvenShape[AuctionRecord] = (auctionId, ownerId, startingPrice, title, status) <>
      (AuctionRecord.tupled, AuctionRecord.unapply)
  }

  val auctions: TableQuery[AuctionTable] = TableQuery[AuctionTable]


  val bidTableName = "bids"

  case class BidRecord(auctionId: UUID, userId: UUID, bidPrice: BigDecimal, timestamp: Long)

  protected class BidTable(tag: Tag) extends Table[BidRecord](tag, bidTableName) {

    def auctionId = column[UUID]("auction_id")

    def userId = column[UUID]("user_id")

    def bidPrice = column[BigDecimal]("bid_price")

    def timestamp = column[Long]("timestamp")

    def pk = primaryKey(s"${bidTableName}_pk", (auctionId, userId))

    override def * : ProvenShape[BidRecord] = (auctionId, userId, bidPrice, timestamp) <> (BidRecord.tupled, BidRecord.unapply)
  }

  val bids: TableQuery[BidTable] = TableQuery[BidTable]


  override def list: Future[List[Auction]] = database.run {
    auctions.joinLeft(bids).on(_.auctionId === _.auctionId).result
  }.map(toView)


  private def toView(seq: Seq[(AuctionRecord, Option[BidRecord])]): List[Auction] = {
    seq.groupBy(_._1).map {
      case (k, v) =>
        Auction(
          k.auctionId,
          k.ownerId,
          k.startingPrice,
          k.title,
          k.status,
          v.toList
            .flatMap(_._2.toList.sortBy(-_.timestamp))
            .map(bid => Bid(bid.userId, bid.bidPrice, bid.timestamp))
        )
    }.toList
  }

  override def upsertAuction(auction: Auction): Future[Done] = database.run {
    auctions.insertOrUpdate(AuctionRecord(auction.auctionId, auction.ownerId, auction.startingPrice, auction.title, auction.status))
  }.map(_ => Done)

  override def updateStatus(auctionId: UUID, status: String): Future[Done] = database.run {
    auctions.filter(_.auctionId === auctionId).map(_.status).update(status)
  }.map(_ => Done)

  override def get(auctionId: UUID): Future[Option[Auction]] = database.run {
    auctions.joinLeft(bids).on(_.auctionId === _.auctionId).filter(_._1.auctionId === auctionId).result
  }.map(toView).map(_.headOption)

  override def upsertBid(auctionId: UUID, bid: Bid): Future[Done] = database.run {
    bids.insertOrUpdate(BidRecord(auctionId, bid.userId, bid.bidPrice, bid.timestamp))
  }.map(_ => Done)

  def init() = {
    database.run {
      for {
       _ <- auctions.schema.create
       _ <- bids.schema.create
      } yield ()
    }
  }
}
