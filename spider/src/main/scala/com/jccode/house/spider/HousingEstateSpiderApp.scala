package com.jccode.house.spider

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.javadsl.SlickSession
import org.jsoup.Jsoup

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

  }

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def fetch(url: String) = Jsoup.connect(url).get

}
