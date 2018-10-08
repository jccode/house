package com.jccode.house.spider

import java.sql.Timestamp

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.github.jccode.house.dao.Tables.{House => HouseItem, _}
import org.jsoup.Jsoup

import scala.concurrent.Future


object SpiderApp extends App {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val session = SlickSession.forConfig("house")
  import session.profile.api._

  val parser = BeiKeParse()

  val sourceSeedUrl = Slick.source(seeds.result).map(_.url)

  val flowModels = Flow[String].map(url => parser.models(fetch(url)))
  val flowRemains = Flow[String].map(url => parser.remainPages(fetch(url)))
    .collect {
      case Some(pages) => pages
    }
    .mapConcat[String](identity)
    .via(flowModels)

  val sinkLog = Sink.foreach(println)
  val sinkDb = Slick.sink[House] { h: House =>
    val r = houses.filter(_.url === h.url).exists.result.flatMap { exist =>
      if (!exist) {
        val now = new Timestamp(System.currentTimeMillis())
        houses += HouseItem(0,
          url = h.url, title = h.title, housingEstate = h.housingEstate, houseType = h.houseType, area = h.area,
          totalPrice = h.totalPrice, unitPrice = h.unitPrice, orientation = h.orientation, decoration = h.decoration,
          elevator = h.elevator, floorDesc = h.floorDesc, age = h.age, subDistrict = h.subDistrict,
          publishDateDesc = h.publishDateDesc, createTime = now, updateTime = now
        )
      } else {
        DBIO.successful(0)
      }
    }

    seeds.result.flatMap { list =>
      DBIO.sequence(
        list.map { i =>
          val now = new Timestamp(System.currentTimeMillis())
          seeds.filter(_.id === i.id).map(_.lastFetchTime).update(Some(now))
        }
      )
    }

    r
  }
  val sinkDb2 = Flow[List[House]].mapConcat(identity).toMat(sinkDb)(Keep.right)


  val g: RunnableGraph[Future[Done]] = RunnableGraph.fromGraph(GraphDSL.create(sinkDb2) { implicit b => sink =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[String](2))
    val merge = b.add(Merge[List[House]](2))

    sourceSeedUrl ~> bcast ~> flowModels ~> merge ~> sink
                     bcast ~> flowRemains ~> merge

    ClosedShape
  })

  val f = g.run()
  f.onComplete(_ => shutdown())

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def fetch(url: String) = Jsoup.connect(url).get

}
