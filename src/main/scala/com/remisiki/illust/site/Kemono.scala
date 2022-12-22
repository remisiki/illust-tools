package com.remisiki.illust.site

trait Kemono {

	lazy val baseUrl = "https://kemono.party/"
	val service: String

	val serviceList = Vector("fanbox", "fantia")
	if (!serviceList.contains(this.service)) {
		throw ServiceNotAvailableException(s"Service ${this.service} not available at kemono")
	}

}

object Kemono {

	val API_PREFIX = "https://kemono.party/api"
	val DATA_PREFIX = "https://kemono.party/data"

}

final case class ServiceNotAvailableException(
	private val message: String = "Service not available at kemono",
	private val cause: Throwable = None.orNull
	) extends Exception(message, cause)