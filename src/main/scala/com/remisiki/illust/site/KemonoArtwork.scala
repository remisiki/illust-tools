package com.remisiki.illust.site

import com.remisiki.illust.util.{Net, UrlFile}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.json.{JSONObject, JSONArray}
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._

class KemonoArtwork(val service: String, val userId: Int, val id: Int)
	extends Artwork with Kemono {

	final private val logger = LoggerFactory.getLogger(getClass)

	def this(json: JSONObject) = {
		this(json.getString("service"), json.getInt("user"), json.getInt("id"))
		this.parse(json)
	}

	var title: String = ""
	var content: String = ""
	var publishedDate: OffsetDateTime = OffsetDateTime.MIN
	var editedDate: OffsetDateTime = OffsetDateTime.MIN
	var addedDate: OffsetDateTime = OffsetDateTime.MIN
	var coverFile: UrlFile = new UrlFile()
	var attachmentFiles: Vector[UrlFile] = Vector.empty

	override def toString(): String = s"Kemono post from ${service} [${this.id}]"

	def parse(): Unit = {
		try {
			val json = new JSONArray(Net.get(s"${Kemono.API_PREFIX}/${this.service}/user/${this.userId}/post/${this.id}"))
			if (json.length() == 0) {
				throw ArtworkNotFoundException(s"Invalid post [${this.id}] for ${this.service} [${this.userId}]")
			}
			val data = json.getJSONObject(0)
			this.parse(data)
		} catch {
			case err: Throwable => {
				this.logger.error("Error parsing", err)
			}
		}
	}

	def parse(json: JSONObject): Unit = {
		try {
			this.title = json.optString("title")
			this.content = json.optString("content")
			this.publishedDate = OffsetDateTime.parse(json.optString("published"), DateTimeFormatter.RFC_1123_DATE_TIME)
			this.editedDate = OffsetDateTime.parse(json.optString("edited"), DateTimeFormatter.RFC_1123_DATE_TIME)
			this.addedDate = OffsetDateTime.parse(json.optString("added"), DateTimeFormatter.RFC_1123_DATE_TIME)
			val fileJson = json.getJSONObject("file")
			this.coverFile = new UrlFile(
				Kemono.DATA_PREFIX + fileJson.optString("path"),
				fileJson.optString("name")
			)
			val attachments = json.getJSONArray("attachments")
			this.attachmentFiles = (0 until attachments.length()).toVector.map {
				i => new UrlFile(
					Kemono.DATA_PREFIX + attachments.getJSONObject(i).optString("path"),
					attachments.getJSONObject(i).optString("name")
				)
			}
		} catch {
			case err: Throwable => throw err
		}
	}

	def getInfo(): String = {
		s"""
			|title: ${this.title}
			|content: ${this.content}
			|publishedDate: ${this.publishedDate}
			|editedDate: ${this.editedDate}
			|addedDate: ${this.addedDate}
			|coverFile: ${this.coverFile}
			|attachmentFiles: ${this.attachmentFiles.foldLeft(""){ (a, b) => s"${a} ${b}" }}
		""".stripMargin
	}

	def downloadSync(path: String = s"./img/kemono/${this.service}/${this.userId}/${this.id}"): Unit = {
		Net.downloadAllSync(this.attachmentFiles :+ this.coverFile, path)
	}

	def downloadAsync(path: String = s"./img/kemono/${this.service}/${this.userId}/${this.id}"): Future[Any] = {
		Net.downloadAllAsync(this.attachmentFiles :+ this.coverFile, path)
	}

}
