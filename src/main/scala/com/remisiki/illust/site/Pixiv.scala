package com.remisiki.illust.site

import com.remisiki.illust.util.UrlFile
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

trait Pixiv {

}

object Pixiv {

	val AJAX_PREFIX = "https://www.pixiv.net/ajax"

	val ORIGINAL_URL_PATTERN = new Regex("""(https://i.pximg.net/img-original/img/\d{4}/\d{2}/\d{2}/\d{2}/\d{2}/\d{2}/)(\d+)_(p|ugoira)(\d+)(\.(jpg|jpe|jpeg|png))""")

	val ORIGINAL_URL_PREFIX = "https://i.pximg.net/img-original/img"

	def getOriginalImages(p: PixivArtwork): Vector[UrlFile] = {
		ORIGINAL_URL_PATTERN.findFirstMatchIn(p.originalUrl) match {
			case Some(matcher: Match) => {
				val prefix: String = matcher.group(1)
				val id: String = matcher.group(2)
				val artType: String = matcher.group(3)
				val suffix: String = matcher.group(5)
				(0 until p.pageCount).toVector.map {i => {
					new UrlFile(s"${prefix}${id}_${artType}${i}${suffix}", headers = p.loginHeaders)
				}}
			}
			case None => Vector.empty
		}
	}

}