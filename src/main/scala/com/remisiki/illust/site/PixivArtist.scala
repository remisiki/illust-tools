package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, NetConnectionException}
import java.nio.file.{Paths, Files}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

class PixivArtist(val id: Int, var name: String = "")(implicit val session: PixivSession = new PixivSession()) extends Artist with PixivAccount {

	lazy val baseUrl = "https://www.pixiv.net/users/"

	override def toString(): String = s"Pixiv user [${this.id}] ${this.name}"

	final private val logger = LoggerFactory.getLogger(getClass)

	if (this.session.id == "") {
		this.logger.warn("Empty Pixiv sessionId, methods won't work")
	}

	def fetchName(): String = {
		this.logger.info(s"Fetching name for [${this.id}]...")
		try {
			val url = s"${PixivArtist.AJAX_PREFIX}${this.id}"
			val userInfo = Net.get(url, this.loginHeaders)
			this.name = userInfo.getJSONObject("body").getString("name")
			this.name
		} catch {
			case err: Throwable => {
				this.logger.error(s"Fetching name for [${this.id}] failed", err)
				""
			}
		}
	}

	def fetchArtworks(): Vector[PixivArtwork] = {
		this.logger.info(s"Fetching artworks for [${this.id}]...")
		try {
			val url = s"${PixivArtist.AJAX_PREFIX}${this.id}/profile/all"
			val profileInfo = Net.get(url, this.loginHeaders)
			profileInfo
				.getJSONObject("body")
				.getJSONObject("illusts")
				.keys()
				.asScala
				.toVector
				.map { key => new PixivArtwork(key.toInt) }
		} catch {
			case err: Throwable => {
				this.logger.error(s"Fetching artworks for [${this.id}] failed", err)
				Vector.empty
			}
		}
	}

	def toKemono(): KemonoArtist = {
		new KemonoArtist("fanbox", this.id, this.name)
	}

}

object PixivArtist {

	val AJAX_PREFIX = "https://www.pixiv.net/ajax/user/"

}