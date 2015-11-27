package util

import java.nio.charset.Charset
import java.nio.file.Files
import java.io.File

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory


/**
  * Created by kentaro.maeda on 2015/11/27.
  */
object DDLMaker {

  private  def exifTime(f:File) = try {
    val meta = ImageMetadataReader.readMetadata(f)
    val date = meta.getDirectory(classOf[ExifSubIFDDirectory]).getDate(
      ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
    Option(date).map(d => String.format("%tY/%tm/%td", d, d, d))
  } catch {
    case e => None
  }

  def main(args:Array[String]) = {

    val files =
    for (dir <-  new File("public/album").listFiles.filter(_.isDirectory);
        jpg <- dir.listFiles() if jpg.isFile && jpg.getName.toLowerCase.endsWith("jpg"))yield{

      (dir.getName, jpg.getName, exifTime(jpg))
    }

    val lines = "# --- !Ups" :: files.map{ t =>
      val (album, name, exif) = (t._1, t._2, t._3.getOrElse(""))
      s"insert into t_photo values('${album}', '${name}', '${exif}', 0, '', false);"
    }.toList ++ ("" :: "# --- !Downs" :: "delete from t_photo" :: Nil)
    import scala.collection.JavaConverters._
    Files.write(new File("conf/evolutions/default/3.sql").toPath, lines.asJava)

  }

}
