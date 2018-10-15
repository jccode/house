package com.jccode.house.spider

import java.sql.Timestamp

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.github.jccode.house.dao.Tables.{House => HouseItem, HousingEstate => HousingEstateItem, _}
import org.jsoup.Jsoup

import scala.concurrent.Future


object HouseSpiderApp {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val session = SlickSession.forConfig("house")
  import session.profile.api._

  def main(args: Array[String]): Unit = {
    val parser = HouseParser()

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
      val now = new Timestamp(System.currentTimeMillis())
      val action1 = houses.filter(_.url === h.url).exists.result.flatMap { exist =>
        if (!exist) {
          houses += HouseItem(0,
            url = h.url, title = h.title, housingEstate = h.housingEstate, houseType = h.houseType, area = h.area,
            totalPrice = h.totalPrice, unitPrice = h.unitPrice, orientation = h.orientation, decoration = h.decoration,
            elevator = h.elevator, floorDesc = h.floorDesc, age = h.age, subDistrict = h.subDistrict,
            publishDateDesc = h.publishDateDesc, tags = h.tags, lastFetchTime = Some(now), createTime = now, updateTime = now
          )
        } else {
          DBIO.successful(0)
        }
      }
      val action2 = housingEstates.filter(_.no === h.housingEstateNo).exists.result.flatMap { exist =>
        if (!exist) {
          housingEstates += HousingEstateItem(0, no = h.housingEstateNo.get, name = h.housingEstate.get, createTime = now, updateTime = now)
        } else {
          DBIO.successful(0)
        }
      }
      action1 >> action2
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
    f.onComplete { _ =>
      updateSourceFetchTime().onComplete(_ => shutdown())
    }
  }

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def fetch(url: String) = Jsoup.connect(url).get

  def updateSourceFetchTime() = {
    session.db.run(
      seeds.result.flatMap { list =>
        DBIO.sequence(
          list.map { i =>
            val now = new Timestamp(System.currentTimeMillis())
            seeds.filter(_.id === i.id).map(_.lastFetchTime).update(Some(now))
          }
        )
      }
    )
  }
}
