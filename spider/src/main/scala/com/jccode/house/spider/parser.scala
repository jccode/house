package com.jccode.house.spider

import org.jsoup.nodes.Document
import play.api.libs.json.{JsResult, Json}

trait Parser[R] {
  def parse(doc: Document): (Seq[R], Option[List[String]])
  def models(doc: Document): Seq[R]
  def remainPages(doc: Document): Option[List[String]]
}


case class BeiKe(name: String)

case class PageData(totalPage: Int, curPage: Int)

object PageData {
  implicit val pageDataReads = Json.reads[PageData]
}

class BeiKeParse extends Parser[BeiKe] {
  import scala.collection.JavaConverters._

  override def parse(doc: Document): (Seq[BeiKe], Option[List[String]]) = (models(doc), remainPages(doc))

  override def models(doc: Document): Seq[BeiKe] = {
    val els = doc.select("body > div.content > div.leftContent > ul > li")
    els.asScala.map {e =>
      val title = e.select("div.info.clear > div.title > a").html
      BeiKe(title)
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
}

object BeiKeParse {
  def apply(): BeiKeParse = new BeiKeParse()
}
