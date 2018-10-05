package com.jccode.house.spider

import org.jsoup.Jsoup
import org.jsoup.nodes.Element


object JsoupTestApp extends App {

  val url = "https://sz.ke.com/ershoufang/futianqu/dp1sf1p2/"
  val doc = Jsoup.connect(url).get()

  println(doc.title)

  val els = doc.select("body > div.content > div.leftContent > ul > li")
  els.forEach {e =>
    val title = e.select("div.info.clear > div.title > a")
    println(title.html)
  }
}
