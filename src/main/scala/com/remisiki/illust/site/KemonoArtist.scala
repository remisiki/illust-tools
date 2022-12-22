package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, NetConnectionException}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import scala.jdk.CollectionConverters._
import org.json.{JSONObject, JSONArray}

class KemonoArtist(val service: String, val id: Int, var name: String = "")
	(implicit val pixivSession: PixivSession = new PixivSession())
	extends Artist with Kemono {

	final private val logger = LoggerFactory.getLogger(getClass)

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

	def fetchArtworks(): Vector[KemonoArtwork] = {
		try {
			val json = new JSONArray(Net.get(s"${Kemono.API_PREFIX}/${this.service}/user/${this.id}"))
			(0 until json.length()).toVector.map {
				i => {
					new KemonoArtwork(json.getJSONObject(i))
				}
			}
		} catch {
			case err: Throwable => {
				this.logger.error(s"Error fetching artworks for ${this}", err)
				Vector.empty
			}
		}
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

	def of(artist: Artist): KemonoArtist = {
		artist match {
			case a: PixivArtist => a.toKemono()
			case a: FantiaArtist => a.toKemono()
			case a => throw ServiceNotAvailableException(s"Service ${a.getClass} not available at kemono")
		}
	}

	def of(artists: Iterable[Artist]): Iterable[KemonoArtist] = {
		artists.map { x => this.of(x) }
	}

}