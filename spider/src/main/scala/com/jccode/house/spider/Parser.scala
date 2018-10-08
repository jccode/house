package com.jccode.house.spider

import org.jsoup.nodes.Document
import play.api.libs.json.Json


case class PageData(totalPage: Int, curPage: Int)

object PageData {
  implicit val pageDataReads = Json.reads[PageData]
}

trait Parser[R] {
  def parse(doc: Document): (List[R], Option[List[String]]) = (models(doc), remainPages(doc))
  def models(doc: Document): List[R]
  def remainPages(doc: Document): Option[List[String]]
}
