package com.jccode.house.spider

import org.jsoup.Jsoup

/**
  * ParserTest
  *
  * @author 01372461
  */
object ParserTest extends App {

  val url = "https://sz.ke.com/ershoufang/futianqu/dp1sf1p2/"
  val doc = Jsoup.connect(url).get()
  val parser = BeiKeParse()
  println(parser.models(doc))
  println(parser.remainPages(doc))

}
