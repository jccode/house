package com.jccode.house.spider

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, MergePreferred, RunnableGraph, Sink, Source, ZipWith}
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


  val seedUrl = Slick.source(seeds.result)

  val flowSeedUrl = Flow[Seed].map(_.url)

  /*
  val parser:Parser[BeiKe] = new CrawlFlow[BeiKe] with BeiKeParse
  val flowParse = Flow[String].map { url =>
    val doc = Jsoup.connect(url).get
    val (modelList, optNextUrls) = parser.parse(doc)
    modelList
  }
  val f = seedUrl.via(flowSeedUrl).via(flowParse).runWith(Sink.foreach(println))
  */



  val crawlFlow = new CrawlFlow[BeiKe] with BeiKeParse

  val f = seedUrl.via(flowSeedUrl).via(crawlFlow.flow()).runWith(Sink.foreach { x =>
    println(x.size)
    println(x)
  })


  f.onComplete(_ => shutdown())

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

}

class CrawlFlow[R] { this : Parser[R] =>

  type Repr = (Seq[R], Option[List[String]])

  def fetch(url: String) = Jsoup.connect(url).get()

  def flow2(): Flow[String, Seq[R], NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val merge = b.add(MergePreferred[String](1))
      val bcast = b.add(Broadcast[Repr](2))
      val par = b.add(Flow[String].map( parse _ compose fetch ))
      val out = b.add(Flow[Repr].map(_._1))
      val back = b.add(Flow[Repr].filter(_._2.isDefined).map(_._2.get).mapConcat(identity))

      merge            ~> par ~> bcast ~> out
      merge.preferred <~ back <~ bcast

      FlowShape(merge.in(0), out.out)
    })

  def flow(): Flow[String, Seq[R], NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val zip = b.add(ZipWith[String, String, List[String]]((left, right) => List(left, right)))
      val bcast = b.add(Broadcast[Repr](2))
      val par = b.add(Flow[List[String]].mapConcat(identity).map( parse _ compose fetch ))
      val out = b.add(Flow[Repr].map(_._1))
      val back = b.add(Flow[Repr].filter(_._2.isDefined).map(_._2.get).mapConcat(identity))

      val concat = b.add(Concat[String]())
      val start = Source.single("")

      /*
      zip.out ~> par ~> bcast ~> out
      zip.in1 <~ back <~ bcast
      */


      zip.out.map { s => println(s); s } ~> par            ~> bcast ~> out
      zip.in1 <~ concat <~ back <~ bcast
                 concat <~ start

      FlowShape(zip.in0, out.out)
    })
}


trait Parser[R] {
  def parse(doc: Document): (Seq[R], Option[List[String]])
}

trait BeiKeParse extends Parser[BeiKe] {
  import scala.collection.JavaConverters._
  override def parse(doc: Document): (Seq[BeiKe], Option[List[String]]) = {
    val els = doc.select("body > div.content > div.leftContent > ul > li")
    val rs = els.asScala.map {e =>
      val title = e.select("div.info.clear > div.title > a").html
      BeiKe(title)
    }
    val nextPage = doc.select("body > div.content > div.leftContent > div.contentBottom.clear > div.page-box.fr > div > a:last-child")
    println(nextPage.html)
    (rs, None)
  }
}

case class BeiKe(name: String)