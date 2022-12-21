package com.remisiki.illust.site

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

class FantiaArtwork(val id: Int) extends Artwork {

	override def toString(): String = s"Fantia post ${this.id}"

	def parse(): Unit = {}

	def downloadSync(): Unit = {}

	def downloadAsync(): Future[Any] = Future { () }

}