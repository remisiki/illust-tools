package com.remisiki.illust.site

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

class FantiaArtwork(val id: Int)
	extends Artwork with Fantia {

	override def toString(): String = s"Fantia post ${this.id}"

	def parse(): Unit = {}

	def downloadSync(path: String = s"./img/fantia/${this.id}"): Unit = {}

	def downloadAsync(path: String = s"./img/fantia/${this.id}"): Future[Any] = Future { () }

	def getInfo(): String = ""

}