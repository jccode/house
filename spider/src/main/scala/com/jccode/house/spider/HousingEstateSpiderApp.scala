package com.jccode.house.spider

import java.sql.Timestamp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.{Flow, Sink}
import org.jsoup.Jsoup
import com.github.jccode.house.dao.Tables._

/**
  * HouseEstateSpiderApp
  *
  * @author 01372461
  */
object HousingEstateSpiderApp {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val session = SlickSession.forConfig("house")
  import session.profile.api._

  def main(args: Array[String]): Unit = {
    val parser = HousingEstateParser()

    val source = Slick.source(housingEstates.filter(x => x.lastFetchTime.isEmpty).result).map(_.no)
    val flowParse = Flow[String].map(parser.parse)
    val sinkLog = Sink.foreach(println)
    val sinkDb = Slick.sink[HousingEstate] { h: HousingEstate =>
      val now = new Timestamp(System.currentTimeMillis())
      housingEstates.filter(_.no === h.no)
        .map(x => (x.avgPrice, x.lowestPrice, x.dealHist, x.sellingCount, x.soldCount, x.district, x.subDistrict, x.lastFetchTime, x.updateTime))
        .update((h.avgPrice, h.lowestPrice, h.dealHist, h.sellingCount, h.soldCount, h.district, h.subDistrict, Some(now), now))
    }

    val f = source.via(flowParse).runWith(sinkDb)

    f.onComplete(_ => shutdown())
  }

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def fetch(url: String) = Jsoup.connect(url).get

}
