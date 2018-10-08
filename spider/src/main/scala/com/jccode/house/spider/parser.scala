package com.jccode.house.spider

import org.jsoup.nodes.Document
import play.api.libs.json.Json



case class House(url: String, title: Option[String] = None, housingEstate: Option[String] = None, houseType: Option[String] = None, area: Option[Float] = None, totalPrice: Option[Double] = None, unitPrice: Option[Double] = None, orientation: Option[String] = None, decoration: Option[String] = None, elevator: Option[String] = None, floorDesc: Option[String] = None, age: Option[Int] = None, subDistrict: Option[String] = None, publishDateDesc: Option[String] = None, tags: Option[String] = None)

case class PageData(totalPage: Int, curPage: Int)

object PageData {
  implicit val pageDataReads = Json.reads[PageData]
}


trait Parser[R] {
  def parse(doc: Document): (Seq[R], Option[List[String]])
  def models(doc: Document): Seq[R]
  def remainPages(doc: Document): Option[List[String]]
}

class BeiKeParse extends Parser[House] {
  import scala.collection.JavaConverters._

  override def parse(doc: Document): (Seq[House], Option[List[String]]) = (models(doc), remainPages(doc))

  override def models(doc: Document): Seq[House] = {
    val els = doc.select("body > div.content > div.leftContent > ul > li")
    els.asScala.map {e =>
      val $title = e.select("div.info.clear > div.title > a")
      val addr = e.select("div.info.clear div.address div.houseInfo").text
      val arrs = addr.split("""\s*\|\s*""")
      val totalPrice = e.select("div.info.clear div.priceInfo div.totalPrice span").text
      val unitPrice = e.select("div.info.clear div.priceInfo div.unitPrice span").text
      val position = e.select("div.info.clear div.flood div.positionInfo").text
      val pos = position.split("""\s*-\s*""")
      val follow = e.select("div.info.clear div.followInfo").text
      val foll = follow.split("""\s*/\s*""")
      val tags = e.select("div.info.clear div.tag > span")
      val tag = tags.asScala.collect {
        case _ @ i if i.className() != "is_vr" => i.text
      }.mkString(",")

      House(
        $title.attr("href"),
        title = Some($title.html),
        housingEstate = arrs.lift(0),
        houseType = arrs.lift(1),
        area = arrs.lift(2).map(extractArea),
        orientation = arrs.lift(3),
        decoration = arrs.lift(4),
        elevator = arrs.lift(5),
        totalPrice = Some(totalPrice.toDouble),
        unitPrice = extractUnitPrice(unitPrice),
        floorDesc = pos.lift(0),
        subDistrict = pos.lift(1),
        age = pos.lift(0).flatMap(extractAge),
        publishDateDesc = foll.lift(2),
        tags = Some(tag)
      )
    }
  }

  override def remainPages(doc: Document): Option[List[String]] = {
    import PageData._
    val nextPage = doc.select("body > div.content > div.leftContent > div.contentBottom > div.page-box > div")
    if (nextPage.isEmpty) {
      return None
    }
    val pageUrl = nextPage.attr("page-url")
    val pageData = Json.fromJson(Json.parse(nextPage.attr("page-data")))
    val host = extractHost(doc.location()).get
    val url = normalUrl(s"$host$pageUrl")
    pageData.asOpt.map { p =>
      ((p.curPage+1) to p.totalPage)
        .map(i => url.replaceAll("""\{page\}""", i.toString))
        .toList
    }
  }

  def extractHost(location: String) = {
    val p = """(http[s]://[\w|.]+/).*""".r
    p.findFirstMatchIn(location).map(_.group(1))
  }

  def normalUrl(url: String) = {
    val p = """([^:])(//)""".r
    p.replaceAllIn(url, "$1/")
  }

  def extractArea(s: String) = s.replaceAll("平米", "").toFloat

  def extractUnitPrice(s: String): Option[Double] = {
    val p = """单价(\d+[\.\d+])元/平米""".r
    p.findFirstMatchIn(s).map(_.group(1).toDouble)
  }

  def extractAge(s: String): Option[Int] = {
    val p = """(\d+)年""".r
    p.findFirstMatchIn(s).map(_.group(1).toInt)
  }
}

object BeiKeParse {
  def apply(): BeiKeParse = new BeiKeParse()
}
