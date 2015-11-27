package controllers

import com.google.inject.Inject
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scala.concurrent.{Future}
import scala.concurrent.ExecutionContext.Implicits.global

class LabelEdit @Inject()(labelDao:LabelDao)  extends Controller with Secured {

  val labelForm = Form(
    "labels" -> seq (
      mapping(
        "id" -> text ,
        "name" -> nonEmptyText)(Label.apply)(Label.unapply)))

  def info = withAuthAsync{ user => implicit request =>
    labelDao.sorted.map { labels =>
      Ok(views.html.label(labelForm.fill(labels)))
    }
  }

  def update = withAuthAsync{ user => implicit request =>
    labelForm.bindFromRequest.fold(
      // on validation error
      errors => Future(BadRequest(views.html.label(errors))),
      // validation OK.
      labels => {
      	// update name
      	labelDao.updateLabels(labels).map { _ =>
          Ok(views.html.label(labelForm.fill(labels)))
        }
      }
    )
  }
}
