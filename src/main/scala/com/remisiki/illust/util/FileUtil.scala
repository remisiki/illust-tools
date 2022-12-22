package com.remisiki.illust.util

import java.io.{File, FileInputStream, FileOutputStream, ByteArrayInputStream}
import java.nio.file.{Files, Paths, Path, StandardCopyOption}
import java.util.zip.{ZipInputStream, ZipEntry}
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageOutputStream
import scala.util.matching.Regex

case class UrlFile(
	val url: String = "",
	var name: String = "",
	val headers: Map[String, String] = Map.empty
	) {

	if (name == "") {
		val r = new Regex("""([^/]*)$""")
		name = {
			r.findFirstMatchIn(url) match {
			case Some(matcher) => matcher.group(1)
			case None => name
			}
		} match {
			case "" => "unknown"
			case x: String => x
		}
	}

	override def toString(): String = s"[ ${name} from ${url} ]"

	def isEmpty(): Boolean = {
		(this.url == "")
	}

}

object FileUtil {

	def makeGifFromZip(zipFileName: String, delay: Int, outputFileName: String, deleteAfterMake: Boolean = false): Unit = {

		// Create files and streams
		val zipFile = new File(zipFileName)
		val fileInputStream = new FileInputStream(zipFile)
		val zipInputStream = new ZipInputStream(fileInputStream)
		val outputFileStream = new FileImageOutputStream(new File(outputFileName))
		var writer: GifSequenceWriter = null

		try {
			// Get first image
			var zipFileEntry = zipInputStream.getNextEntry()
			var bufferedImage = ImageIO.read(zipInputStream)

			// Init gif writer
			writer = new GifSequenceWriter(outputFileStream, bufferedImage.getType(), delay, true)

			writer.writeToSequence(bufferedImage)

			// Unzip and write all images to output
			while ({ zipFileEntry = zipInputStream.getNextEntry(); zipFileEntry != null}) {
				bufferedImage = ImageIO.read(zipInputStream)
				writer.writeToSequence(bufferedImage)
			}
		} catch {
			case err: Throwable => throw err
		} finally {
			// Close streams
			writer.close()
			outputFileStream.close()
			zipInputStream.close()
			// Remove zip file after making gif
			if (deleteAfterMake) {
				zipFile.delete()
			}
		}

	}

}