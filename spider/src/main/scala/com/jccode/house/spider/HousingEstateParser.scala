package com.jccode.house.spider
import java.util.Date

import org.jsoup.nodes.Document


case class HousingEstate(no: String, name: String, avgPrice: Option[Double] = None, lowestPrice: Option[Double] = None, dealHist: Option[String] = None, sellingCount: Option[Int] = None, soldCount: Option[Int] = None, lastFetchTime: Option[java.sql.Timestamp] = None)

case class DealHist(date: Date, price: Double, unitPrice: Double, title: String, period: String)

/**
  * HousingEstateParser
  *
  * @author 01372461
  */
class HousingEstateParser extends Parser[HousingEstate] {

  override def models(doc: Document): List[HousingEstate] = {
    import scala.collection.JavaConverters._

    val avgPrice = doc.select("div.content div.leftContent div#sem_card div.agentCardPush div.agentCardResblockInfo")
    val sellingCount = doc.select("div.content div.leftContent div#sem_card div.agentCardPush div.agentCardResblockInfo div.agentCardResblockDetail div.agentCardDetailItem div.agentCardDetailInfo")
    val soldCount = doc.select("div.content div.leftContent div#sem_card div.agentCardPush div.agentCardResblockInfo div.agentCardResblockDetail div.agentCardDetailItem div.agentCardDetailInfo.LOGCLICK")
    val abc = doc.select("#sem_card > div > div.agentCardResblockInfo")

    println(avgPrice)
    println(sellingCount)
    println(soldCount)
    println(abc.html)

    val histList = doc.select("html body div.content div.leftContent ul.listContent li")
    histList.asScala.map {e =>

    }
    List.empty
  }

  override def remainPages(doc: Document): Option[List[String]] = ???

}

object HousingEstateParser {
  def apply(): HousingEstateParser = new HousingEstateParser()
}
