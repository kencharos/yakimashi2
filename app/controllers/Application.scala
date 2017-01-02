package controllers

import com.google.inject.Inject
import play.api._
import play.api.mvc._
import models._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Future

class Application @Inject()(photoDao:PhotoDao, labelDao: LabelDao) extends Controller with Secured {

  def secureAt(path:String, file:String) = withAuthAsync{user =>
    Assets.at(path, file).apply
  }

  def albums = withAuthAsync{ user => implicit request =>
  	val files = new File("public/album").listFiles();

    photoDao.albumNames().map { als =>
      Ok(views.html.albums(als))
    }

  }

  private def getImages(album:String) = new File("public/album", album).listFiles().filter(_ isFile).sortBy(_ getName).toSeq


  def photo(album:String) = withAuthAsync { user => implicit request => {
  	val images = photoDao.findOneByAlbum(album)

    val labels = labelDao.sorted
    (images.zip(labels)).map{case (p, l) => Ok(views.html.photo(album, p, l))}
  }}

  def print(album:String) = withAuthAsync{ user => implicit request =>
    val photo = photoDao.findOneByAlbum(album)
    val labels = labelDao.sorted

    val data = photo zip labels
    data.map{case (ps, ls) => Ok(views.html.print(album, ps.filterNot(_.noDisp), ls))}

  }
  def sheet(album:String, label:String) = withAuthAsync{ user => implicit request =>
    val data = (photoDao.findByLabel(album, label)).zip(labelDao.sorted)

    data.map{
      case (photos, labels) =>  Ok(views.html.sheet(album, photos, labels.find(_.id == label).get,labels))
    }
  }

}
