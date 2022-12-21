package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, NetConnectionException}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

final case class ServiceNotAvailableException(
	private val message: String = "Service not available at kemono",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)

class KemonoArtist(val service: String, val id: Int, var name: String = "")
	(implicit val pixivSession: PixivSession = new PixivSession()) extends Artist {

	lazy val baseUrl = "https://kemono.party/"

	val serviceList = Vector("fanbox", "fantia")
	if (!serviceList.contains(this.service)) {
		throw ServiceNotAvailableException(s"Service ${this.service} not available at kemono")
	}

	override def toString(): String = s"Kemono artist ${this.name} (from ${this.service} [${this.id}])"

	def fetchName(): String = {
		this.service match {
			case "fanbox" => { this.name = (new PixivArtist(this.id)).fetchName(); this.name }
			case "fantia" => { this.name = (new FantiaArtist(this.id)).fetchName(); this.name }
		}
	}

	def url: String = s"${this.baseUrl}${this.service}/user/${this.id}"

	def exists(): Boolean = {
		Net.ok(this.url)
	}

}

object KemonoArtist {

	final private val logger = LoggerFactory.getLogger(getClass)

	def existsAll(artists: Iterable[KemonoArtist]): Iterable[Boolean] = {
		this.logger.info(s"Start checking ${artists.size} artists...")
		val threads = Future.sequence {
			artists.map {
				artist => Future {
					this.logger.info(s"Checking ${artist}...")
					artist.exists()
				}
			}
		}
		Await.ready(threads, Duration.Inf).value.get match {
			case Success(value) => value
			case Failure(err) => {
				this.logger.error("Error in threads", err)
				Iterable.empty
			}
		}
	}

	def of(artists: Iterable[Artist]): Iterable[KemonoArtist] = {
		artists.map {
			x => x match {
				case a: PixivArtist => a.toKemono()
				case a: FantiaArtist => a.toKemono()
				case a => throw ServiceNotAvailableException(s"Service ${a.getClass} not available at kemono")
			}
		}
	}

}