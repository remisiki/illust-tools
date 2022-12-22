package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, NetConnectionException, UrlFile, FileUtil}
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import org.json.{JSONObject, JSONArray}
import org.jsoup.{Jsoup, HttpStatusException}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await}
import java.nio.file.{Paths}
import scala.concurrent.ExecutionContext.Implicits.global

class PixivArtwork(val id: Int)(implicit val session: PixivSession = new PixivSession())
	extends Artwork with Pixiv with PixivAccount {

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
	var images: Vector[UrlFile] = Vector.empty
	var zipFile: UrlFile = new UrlFile()
	var gifDelay: Int = 0

	def this(json: JSONObject)(implicit session: PixivSession) = {
		this(json.getJSONObject("body").getInt("id"))
	}

	def parse(): Unit = {
		try {
			val url = s"${Pixiv.AJAX_PREFIX}/illust/${this.id}"
			val artWorkInfo = new JSONObject(Net.get(url, this.loginHeaders))
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
				this.images = Pixiv.getOriginalImages(this)
				if (this.isGif()) {
					val meta = new JSONObject(Net.get(s"${Pixiv.AJAX_PREFIX}/illust/${this.id}/ugoira_meta", this.loginHeaders))
					this.zipFile = new UrlFile(meta.getJSONObject("body").getString("originalSrc"), headers = this.loginHeaders)
					this.gifDelay = meta.getJSONObject("body").getJSONArray("frames").getJSONObject(0).getInt("delay")
				}
			}
		} catch {
			case err: Throwable => {
				this.logger.error(s"Error parsing json", err)
			}
		}
	}

	def isGif(): Boolean = {
		Pixiv.ORIGINAL_URL_PATTERN.findFirstMatchIn(this.originalUrl) match {
			case Some(matcher) => {
				(matcher.group(3) == "ugoira")
			}
			case None => false
		}
	}

	override def toString(): String = s"Pixiv artwork [${this.id}] from [${this.userId}]"

	def getInfo(): String = {
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

	def makeGif(path: String = s"./img/pixiv/${this.id}", deleteAfterMake: Boolean = true): Unit = {
		if (!this.zipFile.isEmpty()) {
			this.logger.info(s"Start making GIF for ${this.id}...")
			try {
				FileUtil.makeGifFromZip(
					Paths.get(path, this.zipFile.name).toString,
					this.gifDelay,
					Paths.get(path, s"${this.id}.gif").toString,
					deleteAfterMake
				)
				this.logger.info(s"Finish making GIF for ${this.id}")
			} catch {
				case err: Throwable => {
					this.logger.error(s"Error making GIF for ${this.id}", err)
				}
			}
		}
	}

	def downloadSync(path: String = s"./img/pixiv/${this.id}"): Unit = {
		Net.downloadAllSync(this.images :+ this.zipFile, path)
		this.makeGif(path)
	}

	def downloadAsync(path: String = s"./img/pixiv/${this.id}"): Future[Any] = {
		if (!this.zipFile.isEmpty()) {
			Future.sequence {
				Seq(
					Net.downloadAllAsync(this.images, path),
					Future {
						Net.download(this.zipFile, path)
						this.makeGif(path)
					}
				)
			}
		} else {
			Net.downloadAllAsync(this.images, path)
		}
	}

}
