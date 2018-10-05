package com.jccode.house.spider

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.javadsl.MergePreferred
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink}
import com.github.jccode.house.dao.Tables
import com.github.jccode.house.dao.Tables._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


object SpiderApp extends App {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val session = SlickSession.forConfig("house")
  import session.profile.api._

  val parser:Parser[BeiKe] = new BeiKeParse

  val seedUrl = Slick.source(seeds.result)

  val flowSeedUrl = Flow[Seed].map(_.url)

  val flowParse = Flow[String].map { url =>
    val doc = Jsoup.connect(url).get
    val (modelList, optNextUrls) = parser.parse(doc)
  }



  val f = seedUrl.via(flowSeedUrl).via(flowParse).runWith(Sink.foreach(println))
  f.onComplete(_ => shutdown())

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def parse(url: String): Unit ={
    Jsoup.connect(url).get()
  }


  def flowCrawl[R](flow: Flow[Document, (Seq[R], Option[Seq[String]]), NotUsed]): Flow[String, Seq[R], NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    import akka.stream.scaladsl.GraphDSL.Implicits._

    val merge = b.add(MergePreferred[String](1))
    val bcast = b.add(Broadcast[(Seq[R], Option[Seq[String]])](2))
//    val out = b.add(Flow[(Seq[R], Option[Seq[String]])].filter(_._2.is))

    ???
  })

}


trait Parser[R] {
  def parse(doc: Document): (Seq[R], Option[Seq[String]])
}

class BeiKeParse extends Parser[BeiKe] {
  import scala.collection.JavaConverters._
  override def parse(doc: Document): (Seq[BeiKe], Option[Seq[String]]) = {
    val els = doc.select("body > div.content > div.leftContent > ul > li")
    val rs = els.asScala.map {e =>
      val title = e.select("div.info.clear > div.title > a").html
      BeiKe(title)
    }
    (rs, None)
  }
}

case class BeiKe(name: String)