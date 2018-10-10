package com.jccode.house.spider

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

trait HttpClient {
  def get(url: String): Document = Jsoup.connect(url).get
  def getJson(url: String): String = Jsoup.connect(url).ignoreContentType(true).get.body.text
}

object HttpClient extends HttpClient
