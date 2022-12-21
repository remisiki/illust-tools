package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, NetConnectionException}
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import org.json.{JSONObject, JSONArray}
import org.jsoup.{Jsoup, HttpStatusException}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

class PixivArtwork(val id: Int)(implicit val session: PixivSession = new PixivSession()) extends Artwork with PixivAccount {

	final private val logger = LoggerFactory.getLogger(getClass)

	if (this.session.id == "") {
		this.logger.warn("Empty Pixiv sessionId, methods won't work")
	}

	val baseUrl = "https://www.pixiv.net/artworks/"

	var userId: Int = 0
	var url: String = ""
	var pageCount: Int = 0
	var title: String = ""
	var description: String = ""
	var createDate: OffsetDateTime = OffsetDateTime.MIN
	var uploadDate: OffsetDateTime = OffsetDateTime.MIN
	var aiType: Int = 0
	var width: Int = 0
	var height: Int = 0
	var tags: Vector[String] = Vector.empty
	var viewCount: Int = 0
	var commentCount: Int = 0
	var likeCount: Int = 0
	var bookmarkCount: Int = 0
	var alt: String = ""
	var miniUrl: String = ""
	var thumbUrl: String = ""
	var smallUrl: String = ""
	var regularUrl: String = ""
	var originalUrl: String = ""
	var images: Vector[(String, String)] = Vector.empty

	def this(json: JSONObject)(implicit session: PixivSession) = {
		this(json.getJSONObject("body").getInt("id"))
	}

	def parse(): Unit = {
		try {
			val url = s"${PixivArtwork.AJAX_PREFIX}${this.id}"
			val artWorkInfo: JSONObject = Net.get(url, this.loginHeaders)
			this.parse(artWorkInfo)
		} catch {
			case err: NetConnectionException => {
				this.logger.error(s"Error parsing", err)
			}
		}
	}

	def parse(json: JSONObject): Unit = {
		try {
			if (!json.getBoolean("error")) {
				val body: JSONObject = json.getJSONObject("body")
				this.userId = body.getInt("userId")
				this.url = s"${this.baseUrl}${this.id}"
				this.pageCount = body.getInt("pageCount")
				this.title = body.getString("title")
				this.description = body.getString("description")
				this.createDate = OffsetDateTime.parse(body.getString("createDate"), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
				this.uploadDate = OffsetDateTime.parse(body.getString("uploadDate"), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
				this.aiType = body.getInt("aiType")
				this.width = body.getInt("width")
				this.height = body.getInt("height")
				this.tags = {
					val tags = body.getJSONObject("tags").getJSONArray("tags")
					for (i <- 0 until tags.length()) yield {
						tags.getJSONObject(i).getString("tag")
					}
				}.toVector
				this.viewCount = body.getInt("viewCount")
				this.commentCount = body.getInt("commentCount")
				this.likeCount = body.getInt("likeCount")
				this.bookmarkCount = body.getInt("bookmarkCount")
				this.alt = body.getString("alt")
				val urls = body.getJSONObject("urls")
				this.miniUrl = urls.optString("mini")
				this.thumbUrl = urls.optString("thumb")
				this.smallUrl = urls.optString("small")
				this.regularUrl = urls.optString("regular")
				this.originalUrl = urls.optString("original")
				this.images = PixivArtwork.getOriginalImages(this)
			}
		} catch {
			case err: Throwable => {
				this.logger.error(s"Error parsing json", err)
			}
		}
	}

	override def toString(): String = {
		s"""
		|userId: ${this.userId}
		|url: ${this.url}
		|pageCount: ${this.pageCount}
		|title: ${this.title}
		|description: ${this.description}
		|createDate: ${this.createDate}
		|uploadDate: ${this.uploadDate}
		|aiType: ${this.aiType}
		|width: ${this.width}
		|height: ${this.height}
		|tags: ${this.tags}
		|viewCount: ${this.viewCount}
		|commentCount: ${this.commentCount}
		|likeCount: ${this.likeCount}
		|bookmarkCount: ${this.bookmarkCount}
		|alt: ${this.alt}
		|miniUrl: ${this.miniUrl}
		|thumbUrl: ${this.thumbUrl}
		|smallUrl: ${this.smallUrl}
		|regularUrl: ${this.regularUrl}
		|originalUrl: ${this.originalUrl}
		""".stripMargin
	}

	def downloadSync(): Unit = {
		Net.downloadAllSync(this.images.map(x => x._2), this.loginHeaders, this.images.map(x => x._1), "./img")
	}

	def downloadAsync(): Future[Any] = {
		Net.downloadAllAsync(this.images.map(x => x._2), this.loginHeaders, this.images.map(x => x._1), "./img")
	}

}

object PixivArtwork {

	val AJAX_PREFIX = "https://www.pixiv.net/ajax/illust/"
	val ORIGINAL_URL_PATTERN = new Regex("""(https://i.pximg.net/img-original/img/\d{4}/\d{2}/\d{2}/\d{2}/\d{2}/\d{2}/)(\d+)_p(\d+)(\.(jpg|jpe|jpeg|png|gif))""")
	val ORIGINAL_URL_PREFIX = "https://i.pximg.net/img-original/img/"

	def getOriginalImages(p: PixivArtwork): Vector[(String, String)] = {
		ORIGINAL_URL_PATTERN.findFirstMatchIn(p.originalUrl) match {
			case Some(matcher: Match) => {
				val prefix: String = matcher.group(1)
				val id: String = matcher.group(2)
				val suffix: String = matcher.group(4)
				(0 until p.pageCount).toVector.map {i => {
					val fileName: String = s"${id}_p${i}${suffix}"
					(fileName, s"${prefix}${fileName}")
				}}
			}
			case None => Vector.empty
		}
	}

}