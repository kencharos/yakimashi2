package controllers

import com.google.inject.Inject
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import models._

class PhotoEdit @Inject()(photoDao:PhotoDao) extends Controller with Secured {

	// Json - class transration
	// To convert String - ObjectId, set custom constractor
	implicit val photoFormat = (
			(__ \ "album").format[String] and
			(__ \ "name").format[String] and
			(__ \ "etc").format[Int] and
			(__ \ "comment").format[String] and
			(__ \ "noDisp").format[Boolean] and
			(__ \ "reqs").format[Seq[String]]
		)(
			( album, name, etc, comment, noDisp, labels)
			=> Photo(album, name, etc, comment, noDisp, labels.map(PhotoRequest(album, name, _)))
		,unlift((p:Photo) => Some(p.album, p.name, p.etc, p.comment, p.noDisp, p.reqs.map(_.labelId)))
	)

	val labelDeleteForm = Form(
			"name" -> nonEmptyText
	)

	def info(album:String, name:String) = withAuthAsync{ user => implicit request =>
		val photoFuture = photoDao.findOneByName(album, name)
		photoFuture.map{
			case Some(x) => Ok(Json.toJson(x)).as("application/json")
			case None => Ok(Json.toJson(Photo(album = album, name = name))).as("application/json")
		}

	}

	def update = withAuth(parse.json) { user => implicit request =>
		request.body.validate[Photo].map{
				case p:Photo => {
					photoDao.save(p)
					Ok(p.url)
				}
		}.recoverTotal{
			e => BadRequest("Detected error:"+ JsError(e.errors))
		}
	}

	def deleteLabel(album:String, label:String) = withAuth {user => implicit request =>
		labelDeleteForm.bindFromRequest.fold(
			// on validation error
			errors =>{
				Redirect(routes.Application.sheet(album, label)).flashing{
					"error" -> "error";
				}
			},
			// validation OK.
			value => {
				// update
				photoDao.deleteReqByLabel(album, label)
				Redirect(routes.Application.sheet(album, label))
			}
		)
	}
}
