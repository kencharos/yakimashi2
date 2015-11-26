package controllers

import com.google.inject.Inject
import play.api._
import play.api.mvc._
import models._
import java.io.File
import com.drew.imaging._
import com.drew.metadata.exif.ExifSubIFDDirectory
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

class Application @Inject()(photoDao:PhotoDao, labelDao: LabelDao) extends Controller with Secured {

  def secureAt(path:String, file:String) = withAuthAsync{user =>
    Assets.at(path, file).apply
  }

  def albums = withAuth { user => implicit request =>
  	val files = new File("public/album").listFiles();

  	Ok(views.html.albums(files.filter(_ isDirectory).map(_ getName).sorted));

  }

  private def getImages(album:String) = new File("public/album", album).listFiles().filter(_ isFile).sortBy(_ getName).toSeq

  private  def exifTime(f:File) = try {
    val meta = ImageMetadataReader.readMetadata(f)
    val date = meta.getDirectory(classOf[ExifSubIFDDirectory]).getDate(
      ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
    Option(date)
  } catch {
    case e => None
  }

  def photo(album:String) = withAuthAsync { user => implicit request => {
  	val images = getImages(album)

  	val photos = images.map(f => photoDao.findOneByName(album, f.getName).map{
  	      case None => (Photo(album = album, name = f.getName), exifTime(f))
  	      case Some(p) => (p, exifTime(f))
    })
    val labels = labelDao.sorted

    ((Future.sequence(photos)).zip(labels)).map{case (p, l) => Ok(views.html.photo(album, p, l))}
  }}

  def print(album:String) = withAuthAsync{ user => implicit request =>
    val photo = getImages(album).map{ f =>
      photoDao.findOneByName(album, f.getName).map{
          case None => Photo(album = album, name = f.getName)
          case Some(p)=> p
        }
    }
    val labels = labelDao.sorted

    val data = (Future.sequence(photo)) zip labels
    data.map{case (ps, ls) => Ok(views.html.print(album, ps.filterNot(_.noDisp), ls))}

  }
  def sheet(album:String, label:String) = withAuthAsync{ user => implicit request =>
    val data = (photoDao.findByLabel(album, label)).zip(labelDao.sorted)

    data.map{
      case (photos, labels) =>  Ok(views.html.sheet(album, photos, labels.find(_.id == label).get,labels))
    }
  }

}
