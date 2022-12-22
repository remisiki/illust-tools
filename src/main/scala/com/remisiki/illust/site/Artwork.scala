package com.remisiki.illust.site

import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

trait Artwork {

	val id: Int

	def parse(): Unit

	def downloadSync(path: String = "./img"): Unit

	def downloadAsync(path: String = "./img"): Future[Any]

	def getInfo(): String

}

object Artwork {

	final private val logger = LoggerFactory.getLogger(getClass)

	def downloadAll(artworks: Iterable[Artwork]): Unit = {
		val len = artworks.size
		this.logger.info(s"Start downloading all ${len} artworks...")
		val threads = Future.sequence {
			artworks.map {
				artwork => artwork.downloadAsync()
			}
		}
		Await.ready(threads, Duration.Inf)
		this.logger.info(s"Finish downloading all ${len} artworks")
	}

	def parseAll(artworks: Iterable[Artwork]): Unit = {
		val len = artworks.size
		this.logger.info(s"Start parsing all ${len} artworks...")
		val threads = Future.sequence {
			artworks.map {
				artwork => Future { artwork.parse() }
			}
		}
		Await.ready(threads, Duration.Inf)
		this.logger.info(s"Finish parsing all ${len} artworks")
	}

}

final case class ArtworkNotFoundException(
	private val message: String = "Invalid post",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)