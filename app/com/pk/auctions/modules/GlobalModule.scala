package com.pk.auctions.modules

import com.pk.auctions.actors.CoordinatorActor
import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import slick.jdbc.JdbcBackend._
import play.api.libs.concurrent.AkkaGuiceSupport
import com.pk.auctions.repos.{AuctionRepo, SlickAuctionRepo}
import com.pk.auctions.services._

import scala.concurrent.Await
import scala.concurrent.duration._

class GlobalModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {

    implicit val system = ActorSystem("system")


    val db = Database.forConfig("h2mem1")
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
    val profile = slick.jdbc.H2Profile
    val auctionRepo = new SlickAuctionRepo(profile, db)(ec)
    // dziwne hacki żeby H2 działało:
    Await.result(auctionRepo.init(), 15 seconds)
    bind(classOf[AuctionRepo]).toInstance(auctionRepo)

    val coordinator = system.actorOf(CoordinatorActor.props)

    val processManager = system.actorOf(AuctionProcessManager.props(auctionRepo))

    bind(classOf[ActorRef])
      .annotatedWith(Names.named("Coordinator"))
      .toInstance(coordinator)

    bind(classOf[ActorRef])
      .annotatedWith(Names.named("ProcessManager"))
      .toInstance(processManager)

    bind(classOf[AuctionWriteService]).to(classOf[AuctionWriteServiceImpl])
    bind(classOf[AuctionReadService]).to(classOf[AuctionReadServiceImpl])

  }
}

