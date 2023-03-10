package com.remisiki.illust.util

import java.io.{File, BufferedReader, InputStream, InputStreamReader}
import java.net.{Socket, InetSocketAddress, SocketTimeoutException, URL, HttpURLConnection, URLConnection}
import java.nio.file.{Files, Paths, Path, StandardCopyOption}
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
	def get(url: String, headers: Map[String, String] = Map.empty): String = {
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
			response
		} else {
			throw NetConnectionException(s"Connection to ${url} failed with code ${statusCode}")
		}
	}

	@throws(classOf[HttpStatusException])
	def getHtml(url: String, headers: Map[String, String] = Map.empty): Document = {
		val connection = Jsoup.connect(url).headers(headers.asJava)
		connection.get()
	}

	def download(file: UrlFile, path: String): Unit = {
		if (!file.isEmpty()) {
			Files.createDirectories(Paths.get(path))
			val saveFilePath: Path = Paths.get(path, file.name)
			var inputStream: InputStream = null
			try {
				this.logger.info(s"Downloading ${file.url}")
				val connection: URLConnection = new URL(file.url).openConnection()
				file.headers.foreachEntry((k, v) => {
					connection.setRequestProperty(k, v)
				})
				inputStream = connection.getInputStream()
				Files.copy(inputStream, saveFilePath, StandardCopyOption.REPLACE_EXISTING)
				this.logger.info(s"Downloading ${file.url} to ${saveFilePath} success")
			} catch {
				case err: Throwable => {
					this.logger.error(s"Downloading ${file.url} to ${saveFilePath} error", err)
				}
			} finally {
				inputStream.close()
			}
		}
	}

	def downloadAllSync(files: Iterable[UrlFile], path: String): Unit = {
		val threads: Future[Any] = this.downloadAllAsync(files, path)
		Await.ready(threads, Duration.Inf)
	}

	def downloadAllAsync(files: Iterable[UrlFile], path: String): Future[Any] = {
		Future.sequence {
			files.map {
				file => Future {
					this.download(file, path)
				}
			}
		}
	}

}