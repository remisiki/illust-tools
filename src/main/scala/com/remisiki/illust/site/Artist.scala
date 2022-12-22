package com.remisiki.illust.site

import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

trait Artist {

	val id: Int

	var name: String

	def fetchName(): String

	def fetchArtworks(): Vector[Artwork]

}

object Artist {

	final private val logger = LoggerFactory.getLogger(getClass)

	def fetchAllNames(artists: Iterable[Artist]): Iterable[String] = {
		val len = artists.size
		this.logger.info(s"Fetching all ${len} names...")
		val threads = Future.sequence {
			artists.map {
				artist => Future { artist.fetchName() }
			}
		}
		Await.ready(threads, Duration.Inf).value.get match {
			case Success(value) => {
				this.logger.info(s"Fetching all ${len} names...Done")
				value
			}
			case Failure(err) => {
				this.logger.error(s"Fetching all ${len} names...Error", err)
				Iterable.empty
			}
		}
	}

	def of(fanType: String, id: Int, name: String = ""): Artist = {
		fanType match {
			case "fanbox" => new PixivArtist(id, name)
			case "fantia" => new FantiaArtist(id, name)
			case _ => throw NoSuchArtistTypeException(s"Artist type ${fanType} not found")
		}
	}

}

final case class NoSuchArtistTypeException(
	private val message: String = "Artist type not found",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)

final case class ArtistNotFoundException(
	private val message: String = "Invalid artist",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)
