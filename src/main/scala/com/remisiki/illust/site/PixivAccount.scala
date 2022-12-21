package com.remisiki.illust.site

import com.remisiki.cookies.Chrome
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._

trait PixivAccount extends Loginable {

	implicit val session: PixivSession
	lazy val cookie = s"PHPSESSID=${this.session.id};"

}

object PixivAccount {

	final private val logger = LoggerFactory.getLogger(getClass)

	def login(): PixivSession = {
		this.logger.info("Logging in Pixiv with Chrome cookies...")
		try {
			val browser = new Chrome()
			browser.connect()
			val sessionId: String = {
				val cookies = browser.getCookies("pixiv.net").asScala.toVector
				val filteredCookies = cookies.filter(c => c.getName() == "PHPSESSID")
				if (filteredCookies.length > 0) {
					this.logger.info("Logging in Pixiv with Chrome cookies...Success")
					filteredCookies(0).getValue()
				} else {
					this.logger.warn("Logging in Pixiv with Chrome cookies...Failed")
					""
				}
			}
			browser.close()
			new PixivSession(sessionId)
		} catch {
			case err: Throwable => {
				this.logger.error("Error logging in", err)
				new PixivSession()
			}
		}
	}

}

case class PixivSession(id: String = "")