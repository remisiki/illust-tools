import scala.util.matching.Regex

import com.remisiki.illust.site._
import com.remisiki.illust.util.Sqlite

object Main {

	def main(args: Array[String]): Unit = {
		implicit var pixivSession: PixivSession = new PixivSession()
		var programEnd: Boolean = false
		// val isDigit: Regex = "^[0-9]*$".r
		val db = new Sqlite("data/data.db")
		db.connect()
		var artists: Vector[Artist] = Vector.empty
		while (!programEnd) {
			val input: String = Io.console()
			input match {
				case "q" | "quit" | "exit" => programEnd = true
				case "h" | "help" => Io.printHelp()
				// case isDigit() => ()
				case "a" | "add" => {
					try {
						val fanType = Io.prompt("Input fan site name: ")
						val id = Io.prompt("Input artist id: ").toInt
						val artist = Artist.of(fanType, id)
						artists = artists :+ artist
					} catch {
						case err: NoSuchArtistTypeException => println(err.getMessage)
						case err: java.lang.NumberFormatException => println("Id must be number")
					}
				}
				case "p" | "push" => {
					Artist.fetchAllNames(artists)
					Local.addArtists(db, artists)
					artists = Vector.empty
				}
				case "c" | "check" => {
					val invalidArtists = KemonoArtist.of(Local.getInvalidArtists(db))
					val exists = KemonoArtist.existsAll(invalidArtists)
					invalidArtists.zip(exists)
						.filter(x => x._2)
						.foreach(x => println(s"${x._1} ${x._1.url}"))
				}
				case "d" | "download" => {
					val artworks: Array[PixivArtwork] = {
						val input = Io.prompt("Input Pixiv IDs to download, separated by spaces: ")
						try {
							input.split("\\s+").map(x => new PixivArtwork(x.toInt))
						} catch {
							case err: java.lang.NumberFormatException => println("Id must be number")
							Array.empty
						}
					}
					Artwork.parseAll(artworks)
					Artwork.downloadAll(artworks)
				}
				case "l" | "login" => {
					pixivSession = PixivAccount.login()
				}
				case _ => ()
			}
		}
		db.close()
	}

}
