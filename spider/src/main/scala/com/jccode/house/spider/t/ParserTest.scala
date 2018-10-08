package com.jccode.house.spider.t

import com.jccode.house.spider.{HouseParser, HousingEstateParser}
import org.jsoup.Jsoup

/**
  * ParserTest
  *
  * @author 01372461
  */
object ParserTest extends App {

  def houseParser(): Unit = {
    val url = "https://sz.ke.com/ershoufang/futianqu/dp1sf1p2/"
    val doc = Jsoup.connect(url).get()
    val parser = HouseParser()
    println(parser.models(doc))
    println(parser.remainPages(doc))
    //  parser.models(doc).foreach(println)
  }

  def housingEstateParser(): Unit = {
    val url = "https://sz.ke.com/chengjiao/c2411050506921/"
    val doc = Jsoup.connect(url).get()
    val parser = HousingEstateParser()
    println(parser.models(doc))
  }

  housingEstateParser()
}
