package com.jccode.house.spider
import play.api.libs.json.Json


case class HousingEstate(no: String, name: String, avgPrice: Option[Double] = None, lowestPrice: Option[Double] = None, sellingCount: Option[Int] = None, soldCount: Option[Int] = None, dealHist: Option[String] = None)

case class DealHist(date: String, price: String, unitPrice: String, title: String, period: String) {
  override def toString: String = s"$date|$price|$unitPrice|$title|$period"
}

case class BaseInfoData(name: String, districtName: String, bizcircleName: String, unitPrice: Double, `90saleCount`: Int, sellNum: String)

object BaseInfoData {
  implicit val baseInfoDataReads = Json.reads[BaseInfoData]
}

/**
  * HousingEstateParser
  *
  * @author 01372461
  */
class HousingEstateParser {

  def baseInfoUrl(housingEstateNo: String) = s"https://sz.ke.com/chengjiao/listtop?resblock_id=$housingEstateNo"
  def histListUrl(housingEstateNo: String) = s"https://sz.ke.com/chengjiao/c$housingEstateNo/"

  def parse(housingEstateNo: String): HousingEstate = {
    import scala.collection.JavaConverters._

    val baseInfoJson = HttpClient.getJson(baseInfoUrl(housingEstateNo))
    val baseInfo: Option[BaseInfoData] = Json.fromJson[BaseInfoData]((Json.parse(baseInfoJson) \ "data" \ "info").get).asOpt

    val doc = HttpClient.get(histListUrl(housingEstateNo))
    val histList = doc.select("body > div.content > div.leftContent > ul > li")
    val list = histList.asScala.map {e =>
      val date = e.select("div.info div.address div.dealDate").text
      val price = e.select("div.info div.address div.totalPrice span.number").text
      val unitPrice = e.select("div.info div.flood div.unitPrice span.number").text
      val title = e.select("div.info div.title a").text
      val period = e.select("div.info div.dealCycleeInfo span.dealCycleTxt").text

      DealHist(
        date = date,
        price = price,
        unitPrice = unitPrice,
        title = title,
        period = period
      )
    }

    val lowest = list.map(x => toDouble(x.unitPrice)).fold(Double.MaxValue) {(prev, curr) => prev.min(curr)}
    val dealHist = list.mkString(";\n")

    HousingEstate(no = housingEstateNo,
      name = baseInfo.get.name,
      avgPrice = baseInfo.map(_.unitPrice),
      lowestPrice = Some(lowest),
      sellingCount = baseInfo.map(_.sellNum.toInt),
      soldCount = baseInfo.map(_.`90saleCount`),
      dealHist = Some(dealHist),
    )
  }

  def toDouble(x: String) = {
    try {
      x.toDouble
    } catch {
      case _: NumberFormatException =>
        Double.MaxValue
    }
  }

}

object HousingEstateParser {
  def apply(): HousingEstateParser = new HousingEstateParser()
}
