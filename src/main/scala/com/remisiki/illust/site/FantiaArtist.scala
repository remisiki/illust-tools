package com.remisiki.illust.site

import com.remisiki.illust.util.Net
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}

class FantiaArtist(val id: Int, var name: String = "") extends Artist {

	lazy val baseUrl = "https://fantia.jp/fanclubs/"

	override def toString(): String = s"Fantia user [${this.id}] ${this.name}"

	final private val logger = LoggerFactory.getLogger(getClass)

	def fetchName(): String = {
		this.logger.info(s"Fetching name for [${this.id}]...")
		try {
			val url = s"${this.baseUrl}${this.id}"
			val document = Net.getHtml(url)
			this.name = document.title().replace("｜ファンティア[Fantia]", "")
			this.name
		} catch {
			case err: Throwable => {
				this.logger.error(s"Fetching name for [${this.id}] failed", err)
				""
			}
		}
	}

	def fetchArtworks(): Vector[FantiaArtwork] = {
		this.logger.info(s"Fetching artworks for [${this.id}]...")
		try {
			var url = s"${this.baseUrl}${this.id}/posts"
			var document = Net.getHtml(url)
			var aTags = document.select("a.page-link")
			val pageCount: Int = aTags.last.attr("href").replace(s"/fanclubs/${this.id}/posts?page=", "").toInt
			val threads = Future.sequence {
				(1 to pageCount).map { i => {
					var artworks: Vector[Int] = Vector.empty
					if (i != 1) {
						url = s"${this.baseUrl}${this.id}/posts?page=${i}"
						document = Net.getHtml(url)
					}
					aTags = document.select(".row.row-packed.row-eq-height a.link-block")
					aTags.forEach(a => artworks = artworks :+ a.attr("href").replace("/posts/", "").toInt)
					Future { artworks }
				}}
			}
			Await.ready(threads, Duration.Inf).value.get match {
				case Success(value) => {
					value
						.foldLeft(Vector.empty: Vector[Int]) { (x, y) => x ++ y }
						.map { x => new FantiaArtwork(x) }
				}
				case Failure(err) => throw err
			}
		} catch {
			case err: Throwable => {
				this.logger.error(s"Fetching artworks for [${this.id}] failed", err)
				Vector.empty
			}
		}
	}

	def toKemono(): KemonoArtist = {
		new KemonoArtist("fantia", this.id, this.name)
	}

}
