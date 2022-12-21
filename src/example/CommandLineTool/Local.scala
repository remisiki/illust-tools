import com.remisiki.illust.site._
import com.remisiki.illust.util.Sqlite

object Local {

	def addArtists(db: Sqlite, artists: Iterable[Artist]): Unit = {
		val requests: Array[String] = artists.map {
			x => {
				val fanType: Int = x match {
					case _: PixivArtist => 1
					case _: FantiaArtist => 2
					case _: Artist => 0
				}
				if (fanType == 0) {
					""
				} else {
					s"""
						INSERT INTO kemonoFavorite(type, name, pid)
						VALUES(
							${fanType},
							"${x.name}",
							${x.id}
						)
					"""
				}
			}
		}.toArray
		db.executeBatch(requests)
	}

	def getInvalidArtists(db: Sqlite): Vector[Artist] = {
		val rs = db.query(
			"""
				SELECT
					f.name, k.pid, k.name
				FROM kemonoFavorite k
				JOIN fanType f ON k.type = f.id
				WHERE valid = 0
			"""
		)
		rs.map { row =>
			Artist.of(row(0), row(1).toInt, row(2))
		}.toVector
	}

	// def update(): Type = ???

}