package com.remisiki.illust.site

trait Loginable {

	val baseUrl: String

	val cookie: String

	lazy val loginHeaders: Map[String, String] = Map(
		"referer" -> this.baseUrl,
		"user-agent" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36",
		"cookie" -> this.cookie
	)

}