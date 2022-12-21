import scala.io.{StdIn,AnsiColor}

object Io {

	def console(): String = {
		print(s"${AnsiColor.GREEN}$$ ${AnsiColor.RESET}")
		StdIn.readLine()
	}

	def prompt(message: String): String = {
		print(message)
		StdIn.readLine()
	}

	def printHelp(): Unit = {
		println("""
			|This is a tool that can download illusts from fan sites (Pixiv, Fantia, Kemono).
			|Command lists:
			|h (help): Show this help message.
			|q (quit, exit): Quit.
			|a (add): Add one artist to the queue
			|p (push): Push artist queue to local database
			|c (check): Check existence of artists at kemono from local database
			|d (download): Download Pixiv illusts
			""".stripMargin)
	}

}