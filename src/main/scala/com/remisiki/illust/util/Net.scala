package com.remisiki.illust.util

import java.awt.image.BufferedImage
import java.io.{File, BufferedReader, InputStream, InputStreamReader}
import java.net.{Socket, InetSocketAddress, SocketTimeoutException, URL, HttpURLConnection, URLConnection}
import java.nio.file.{Files, Paths}
import java.util.Iterator
import javax.imageio.stream.ImageInputStream
import javax.imageio.{ImageIO, ImageReader}
import org.json.{JSONObject, JSONArray}
import org.jsoup.nodes.Document
import org.jsoup.{Jsoup, HttpStatusException}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

final case class NetConnectionException(
	private val message: String = "Connection failed",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)

object Net {

	final private val logger = LoggerFactory.getLogger(getClass)

	def ok(url: String, headers: Map[String, String] = Map.empty): Boolean = {
		try {
			val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
			headers.foreachEntry((k, v) => {connection.setRequestProperty(k, v)})
			connection.setInstanceFollowRedirects(false)
			val statusCode = connection.getResponseCode()
			(statusCode == 200)
		} catch {
			case err: Throwable => {
				this.logger.error(s"Connection to ${url} failed", err)
				false
			}
		}
	}

	@throws(classOf[NetConnectionException])
	def get(url: String, headers: Map[String, String] = Map.empty): JSONObject = {
		val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
		headers.foreachEntry((k, v) => {connection.setRequestProperty(k, v)})
		val statusCode = connection.getResponseCode()
		if (statusCode == 200) {
			val reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))
			var response = ""
			var line = ""
			while ({line = reader.readLine(); line} != null) {
				response = response + line
			}
			reader.close()
			new JSONObject(response)
		} else {
			throw NetConnectionException(s"Connection to ${url} failed with code ${statusCode}")
		}
	}

	@throws(classOf[HttpStatusException])
	def getHtml(url: String, headers: Map[String, String] = Map.empty): Document = {
		val connection = Jsoup.connect(url).headers(headers.asJava)
		connection.get()
	}

	def download(url: String, headers: Map[String, String], fileName: String, path: String): Unit = {
		Files.createDirectories(Paths.get(path))
		val saveFileName: String = Paths.get(path, fileName).toString
		try {
			this.logger.info(s"Downloading ${url}")
			val connection: URLConnection = new URL(url).openConnection()
			headers.foreachEntry((k, v) => {
				connection.setRequestProperty(k, v)
			})
			val inputStream: InputStream = connection.getInputStream()
			val imageInputStream: ImageInputStream = ImageIO.createImageInputStream(inputStream)
			val imageReader: Iterator[ImageReader] = ImageIO.getImageReaders(imageInputStream)
			if (imageReader.hasNext()) {
				val imageFormat: String = imageReader.next().getFormatName().toLowerCase()
				val bufferedImage: BufferedImage = ImageIO.read(imageInputStream)
				ImageIO.write(bufferedImage, imageFormat, new File(saveFileName))
			}
			inputStream.close()
			this.logger.info(s"Downloading ${url} to ${saveFileName} success")
		} catch {
			case err: Throwable => {
				this.logger.error(s"Downloading ${url} to ${saveFileName} error", err)
			}
		}
	}

	def downloadAllSync(urls: Iterable[String], headers: Map[String, String], fileNames: Iterable[String], path: String): Unit = {
		val threads: Future[Any] = this.downloadAllAsync(urls, headers, fileNames, path)
		Await.ready(threads, Duration.Inf)
	}

	def downloadAllAsync(urls: Iterable[String], headers: Map[String, String], fileNames: Iterable[String], path: String): Future[Any] = {
		if (urls.size != fileNames.size) {
			throw new Exception("Url and filename size inconsistent")
		}
		Future.sequence {
			urls.zip(fileNames).map {
				params => Future {
					this.download(params._1, headers, params._2, path)
				}
			}
		}
	}

}