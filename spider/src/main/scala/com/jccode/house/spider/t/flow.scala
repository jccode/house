package com.jccode.house.spider.t

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, MergePreferred, Source, ZipWith}
import com.jccode.house.spider.Parser
import org.jsoup.Jsoup


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


