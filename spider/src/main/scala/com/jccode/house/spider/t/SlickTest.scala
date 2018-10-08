package com.jccode.house.spider.t

import java.sql.Timestamp

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Await

/**
  * SlickTest
  *
  * @author 01372461
  */
object SlickTest extends App {
  import com.github.jccode.house.dao.Tables._
  import profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  val config = DatabaseConfig.forConfig[JdbcProfile]("house")
  val db = config.db

  val action: DBIOAction[Seq[Int], NoStream, Effect.Read with Effect.Write] = seeds.result.flatMap { list =>
    DBIO.sequence(
      list.map { i =>
        val now = new Timestamp(System.currentTimeMillis())
        seeds.filter(_.id === i.id).map(_.lastFetchTime).update(Some(now))
      }
    )
  }

  val f = db.run(action)

  Await.result(f, 5 seconds)
}
