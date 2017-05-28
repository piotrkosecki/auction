package com.pk.auctions.controllers

import java.util.UUID

import com.pk.auctions.actors.AuctionProtocol._
import com.pk.auctions.actors.CoordinatorActor.{BidAuction, CreateAuctionProtocol}
import com.google.inject.Inject
import com.pk.auctions.models.{BidAuctionRequest, CreateAuctionRequest}
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import com.pk.auctions.services.{AuctionReadService, AuctionWriteService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuctionController @Inject()(auctionWriteService: AuctionWriteService,
                                  auctionReadService: AuctionReadService)  extends Controller {


  // można zrobić lepszą walidację
  def withBodyAs[A](thunk: A => Future[Result])
                   (implicit request: Request[AnyContent], reads: Reads[A]): Future[Result] = {
    request.body.asJson.flatMap(_.validate[A].asOpt) match {
      case Some(value) => thunk(value)
      case None => Future.successful(BadRequest)
    }
  }

  def listAuctions = Action.async {
    auctionReadService.list.map { auctions =>
      Ok(Json.toJson(auctions))
    }
  }

  def getAuction(auctionId: UUID) = Action.async {
    auctionReadService.get(auctionId).map { auction =>
      Ok(Json.toJson(auction))
    }
  }

  def createAuction(userId: UUID) = Action.async { implicit request =>
    withBodyAs[CreateAuctionRequest] { createAuctionReq =>
      val createAuction = CreateAuctionProtocol(
        createAuctionReq.auctionId.getOrElse(UUID.randomUUID()),
        createAuctionReq.startingPrice,
        createAuctionReq.durationInSeconds,
        userId,
        createAuctionReq.title
      )
      auctionWriteService.createAuction(createAuction).map {
        case _: AuctionCreated => Created(Json.obj("auctionId" ->createAuction.auctionId.toString))
        case AuctionNotCreated(msg, _) => BadRequest(Json.obj("error" -> msg))
        case _ => InternalServerError
      }
    }
  }

  def bid(auctionId: UUID) = Action.async { implicit request =>
    withBodyAs[BidAuctionRequest] { bidReq =>
      auctionWriteService.bid(BidAuction(auctionId, bidReq.price, bidReq.userId)).map {
        case _: BidAccepted => Ok
        case BidRejected(msg, _) => BadRequest(Json.obj("error" -> msg))
        case _ => InternalServerError
      }
    }
  }

}