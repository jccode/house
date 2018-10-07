package com.jccode.house.spider

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source}

object CircleGraphTestApp extends App {

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher

  def graphTest(): Unit = {
    val g: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val source = Source(1 to 100)
      val sink = Sink.foreach(println)

      val f1 = Flow[Int]
      val f2 = Flow[Int].map { x =>
        if (x % 3 == 0) { (x, Some(x.toString)) }
        else (x, None)
      }

      val merge = builder.add(MergePreferred[Int](1))
      val bcast = builder.add(Broadcast[(Int, Option[String])](2))

      val outFilter = Flow[(Int, Option[String])].filter(_._2.isEmpty)
      val notOutFilter = Flow[(Int, Option[String])].filter(_._2.isDefined).map{_._2.get.toInt * 2 - 1}

      source ~> f1 ~> merge           ~> f2           ~> bcast ~> outFilter ~> sink
      merge.preferred <~ notOutFilter <~ bcast

      ClosedShape
    })

    g.run()

    actorSystem.terminate()
  }

  def reusebleGraphTest(): Unit = {

    def processFlow(flow: Flow[Int, (Int, Option[String]), NotUsed]): Flow[Int, Int, NotUsed] = {

      val f: Flow[Int, Int, NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        val merge = b.add(MergePreferred[Int](1))
        val bcast = b.add(Broadcast[(Int, Option[String])](2))

        val outFilter =
          b.add(Flow[(Int, Option[String])].map(_._1))


        val notOutFilter =
          b.add(Flow[(Int, Option[String])].filter(_._2.isDefined).map{_._2.get.toInt * 2 - 1})

        merge           ~> flow         ~> bcast ~> outFilter
        merge.preferred <~ notOutFilter <~ bcast

        FlowShape(merge.in(0), outFilter.out)
      })
      f
    }

    val source = Source(1 to 100)
    val sink = Sink.foreach(println)
    val f2 = Flow[Int].map { x =>
      if (x % 3 == 0) { (x, Some(x.toString)) }
      else (x, None)
    }

    val g = source.via(processFlow(f2)).to(sink)
    g.run()
    actorSystem.terminate()
  }

  reusebleGraphTest()
}
