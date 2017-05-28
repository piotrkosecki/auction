package com.pk.auctions.services

import java.util.UUID

import com.google.inject.Inject
import com.pk.auctions.models.Auction
import com.pk.auctions.repos.AuctionRepo

import scala.concurrent.Future


trait AuctionReadService {

  def list: Future[List[Auction]]

  def get(auctionId: UUID): Future[Option[Auction]]

}

class AuctionReadServiceImpl @Inject() (auctionRepo: AuctionRepo) extends AuctionReadService {
  override def list: Future[List[Auction]] = auctionRepo.list

  override def get(auctionId: UUID): Future[Option[Auction]] = auctionRepo.get(auctionId)
}