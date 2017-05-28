package com.pk.auctions.actors

import java.util.UUID

import com.pk.auctions.actors.AuctionActor.{AuctionFinishedState, AuctionInProgressState, AuctionNotExistState, AuctionState}
import com.pk.auctions.actors.AuctionProtocol._
import akka.actor.{ActorLogging, Props}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, RecoveryCompleted}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object AuctionActor {

  sealed trait AuctionState
  case object AuctionNotExistState extends AuctionState
  case object AuctionInProgressState extends AuctionState
  case object AuctionFinishedState extends AuctionState

  def props(id: UUID): Props = Props(new AuctionActor(id))
}

class AuctionActor(id: UUID) extends PersistentActor with ActorLogging {

  private var state: AuctionState = AuctionNotExistState

  // state with placeholder values placeholders
  private var title: String = ""

  private var ownerId: UUID = UUID.randomUUID()

  private var highestBidder: Option[UUID] = None

  private var price: BigDecimal = BigDecimal(0)

  override def receiveRecover: Receive = LoggingReceive {
    case event: AuctionEvent =>
      updateState(event)

    case RecoveryCompleted =>
      state match {
        case AuctionNotExistState =>
          context.become(nonExisting)
        case AuctionInProgressState =>
          context.become(inProgress)
        case AuctionFinishedState =>
          context.become(finished)
      }
  }

  override def receiveCommand: Receive = nonExisting

  def nonExisting: Receive = LoggingReceive {

    case CreateAuctionCommand(aId,p,ttl,o,t) =>
      persist(AuctionCreated(aId,p,ttl,o,t)) { event =>
        logAndUpdateState(event)
        sender() ! event
        context.become(inProgress)
      }
    case msg =>
      log.info(s"Unrecognized message $msg")
  }

  def inProgress: Receive = LoggingReceive {
    case CreateAuctionCommand(_,_,_,_,_) =>
      sender() ! AuctionNotCreated("auction already exist")

    case BidCommand(p, _) if p <= price =>
      sender() ! BidRejected("price is lower or equal than current price")

    case BidCommand(_, u) if u == ownerId =>
      sender() ! BidRejected("you cannot bid on your own auction")

    case BidCommand(p, u) =>
      persist(BidAccepted(id, p, u)) { event =>
        logAndUpdateState(event)
        sender() ! event
      }

    case FinishAuctionCommand =>
      persist(AuctionFinished(id)) { event =>
        logAndUpdateState(event)
        context.become(finished)
      }
  }

  def finished: Receive = LoggingReceive {
    case CreateAuctionCommand(_,_,_,_,_) =>
      sender() ! AuctionNotCreated("auction already exist and its finished")

    case BidCommand(_,_) =>
      sender() ! BidRejected("auction already finished")
  }


  private def logAndUpdateState(event: AuctionEvent): Unit = {
    log.info(s"Auction actor received event: $event")
    updateState(event)
  }

  private def updateState(event: AuctionEvent): Unit = {
    state match {
      case AuctionNotExistState =>
        event match {
          case AuctionCreated(_, startingPrice, durationInSeconds, ownrId, ttle, _) =>
            title = ttle
            ownerId = ownrId
            price = startingPrice
            context.system.scheduler.scheduleOnce(durationInSeconds seconds, self, FinishAuctionCommand)
            state = AuctionInProgressState
          case cmd =>
            log.error(s"Wrong command for this state: $cmd")
        }

      case AuctionInProgressState =>
        event match {
          case BidAccepted(_, offeredPrice, userId, _) =>
            price = offeredPrice
            highestBidder = Some(userId)
          case AuctionFinished(_, _) =>
            state = AuctionFinishedState
          case evt =>
            log.warning(s"Not relevant event: $evt")
        }

      case AuctionFinishedState =>
        log.warning(s"Auction in the finished state")
    }
  }

  override def persistenceId: String = s"auction-${self.path.name}"
}

object AuctionProtocol {

  sealed trait AuctionCommand

  case class CreateAuctionCommand(auctionId: UUID, startingPrice: BigDecimal, durationInSeconds: Int, ownerId: UUID, title: String) extends AuctionCommand

  case class BidCommand(offeredPrice: BigDecimal, userId: UUID) extends AuctionCommand

  case object FinishAuctionCommand extends AuctionCommand


  sealed trait AuctionEvent {
    val timestamp: Long
  }

  case class AuctionCreated(auctionId: UUID, startingPrice: BigDecimal,
                            durationInSeconds: Int,
                            ownerId: UUID,
                            title: String,
                            timestamp: Long = System.currentTimeMillis()) extends AuctionEvent

  case class AuctionNotCreated(reason: String, timestamp: Long = System.currentTimeMillis()) extends AuctionEvent

  case class AuctionFinished(auctionId: UUID, timestamp: Long = System.currentTimeMillis()) extends AuctionEvent

  case class BidAccepted(auctionId: UUID, offeredPrice: BigDecimal, userId: UUID, timestamp: Long = System.currentTimeMillis()) extends AuctionEvent

  case class BidRejected(reason: String, timestamp: Long = System.currentTimeMillis()) extends AuctionEvent

}