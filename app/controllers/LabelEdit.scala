package controllers

import com.google.inject.Inject
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.Await
import scala.concurrent.duration._

class LabelEdit @Inject()(labelDao:LabelDao)  extends Controller with Secured {

  val labelForm = Form(
    "labels" -> seq (
      mapping(
        "id" -> text ,
        "name" -> nonEmptyText)(Label.apply)(Label.unapply)))

  def info = withAuth{ user => implicit request =>
    val labels = Await.result(labelDao.sorted, 5 seconds)
    Ok(views.html.label(labelForm.fill(labels)))

  }

  def update = withAuth{ user => implicit request =>
    labelForm.bindFromRequest.fold(
      // on validation error
      errors => BadRequest(views.html.label(errors)),
      // validation OK.
      labels => {
      	// update name
      	labels.foreach{labelDao.update}
      	Ok(views.html.label(labelForm.fill(labels)))
      }
    )
  }
}
