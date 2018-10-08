package com.jccode.house.spider

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink}
import akka.stream.{ActorMaterializer, ClosedShape}
import com.github.jccode.house.dao.Tables._
import org.jsoup.Jsoup


object SpiderApp extends App {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher
  implicit val session = SlickSession.forConfig("house")
  import session.profile.api._

  val parser = BeiKeParse()

  val sourceSeedUrl = Slick.source(seeds.result).map(_.url)

  //val f = sourceSeedUrl.runWith(Sink.foreach(println))
  //f.onComplete(_ => shutdown())

  //val flowModels = Flow.fromFunction[String, Seq[BeiKe]](url => parser.models(fetch(url)))
  val flowModels = Flow[String].map(url => parser.models(fetch(url)))
  val flowRemains = Flow[String].map(url => parser.remainPages(fetch(url)))
    .collect {
      case Some(pages) => pages
    }
    .mapConcat[String](identity)
    .via(flowModels)

  val sinkLog = Sink.foreach(println)

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[String](2))
    val merge = b.add(Merge[Seq[BeiKe]](2))

    sourceSeedUrl ~> bcast ~> flowModels ~> merge ~> sinkLog
                     bcast ~> flowRemains ~> merge

    ClosedShape
  })

  g.run()
  //shutdown()

  def shutdown(): Unit = {
    session.close()
    materializer.shutdown()
    actorSystem.terminate()
  }

  def fetch(url: String) = Jsoup.connect(url).get

}
