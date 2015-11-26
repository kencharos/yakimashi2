package models

import com.google.inject.{Singleton, Inject}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
import slick.dbio.DBIOAction
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future


case class User(id:String, password:String)

/**
 * Slickによる DAOの定義
 *
 * Slickではテーブル定義用にTableを継承したクラスと、それを元にした TableQueryを作るのが基本。
 * 作り方は色々あり、必ずしもDAOにする必要は無く、TableQueryを直接公開するのもあり。
 * play 2.4のslickに関するドキュメントは、slickの知識がないとよくわからない。
 * play-slickのサンプルコードの方が充実しているので、そちらを参照した方がいい。
 */
@Singleton()
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  // クエリ組み立て部品
  private val Users = TableQuery[UsersTable]

  def find(id:String):Future[Option[User]] = {
    // tablequery はモナド的にクエリの組み立てが可能。
    val q = for(u <- Users; if u.id === id)yield(u)
    // resultでクエリをDB操作に変換し、db.rubで実行するが、基本的にFutureを返すことに注意。
    db.run(q.result.headOption)
  }
  // テーブル定義。必須。
  private class UsersTable(tag: Tag) extends Table[User](tag, "t_user") {
    // IDは大文字である必要がある。
    def id = column[String]("id", O.PrimaryKey)

    def password = column[String]("password")
    // SQL取得結果とcase classへのマッピング
    def * = (id, password) <>(User.tupled, User.unapply _)
  }
}


case class Label(id:String, name:String)
@Singleton()
class LabelDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Labels = TableQuery[LabelsTable]

  def findOne(id:String) = {
    db.run(Labels.filter(_.id === id).result.headOption)
  }

  def sorted:Future[Seq[Label]] = db.run(Labels.sortBy(_.id).result)

  def update(label:Label) = db.run(Labels.filter(_.id === label.id).map(_.name).update(label.name))

  // テーブル定義。必須。
  private class LabelsTable(tag: Tag) extends Table[Label](tag, "t_label") {
    // IDは大文字である必要がある。
    def id = column[String]("id", O.PrimaryKey)

    def name = column[String]("name")
    def * = (id, name) <>(Label.tupled, Label.unapply _)
  }
}

case class PhotoInner(album:String,
                 name:String,
                 etc:Int = 0,
                 comment:String = "",
                 noDisp:Boolean = false){
  def url = "album/" + album + "/" + name
}

case class Photo(album:String,
                 name:String,
                 etc:Int = 0,
                 comment:String = "",
                 noDisp:Boolean = false,
                 reqs:Seq[PhotoRequest] = Seq()){
  def url = "album/" + album + "/" + name
  def count = reqs.size + etc
}


case class PhotoRequest(album:String, name:String, labelId:String)

@Singleton()
class PhotoDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Photos = TableQuery[PhotosTable]

  private val PhotoRequests = TableQuery[PhotoRequestTable]

  def findOneByName(album:String, name:String):Future[Option[Photo]] = {
    val q =Photos.filter(p => p.album === album && p.name === name)
      .joinLeft(PhotoRequests).on {case (a,b) => a.album === b.album && a.name === b.name}

    db.run(q.result).map{
      case x if x.size == 0 => None
      case x => {
        val p = x.head._1
        val reqs = x.map(_._2).flatten // List[Option[A]] -> List[A]
        Some(Photo(p.album, p.name, p.etc, p.comment, p.noDisp, reqs))
      }
    }
  }

  def findByLabel(album:String, label:String) = {
    val query =
      for (p <- Photos if (p.album === album)
      ) yield (p)
    // subquery exisits.
    db.run(query.filter(p => PhotoRequests.filter(r => r.labelId === label && p.name === r.name && p.album===r.album).exists).result)
  }

  def deleteReqByLabel(album:String, labelId:String) = {
    val q = PhotoRequests.filter(r => r.album === album && r.labelId ===labelId)

    db.run(q.delete)
  }

  private def deleteReq(p:Photo) = {
    PhotoRequests.filter(r => r.album === p.album && r.name === p.name).delete
  }
  private def insertReq(p:Photo) = {
    PhotoRequests ++= p.reqs
  }

  def save(photo:Photo) = {
    val p = PhotoInner(photo.album, photo.name, photo.etc, photo.comment, photo.noDisp)
    def innerSave(pi:PhotoInner) = {
      Photos.filter(p => p.album === photo.album && p.name === photo.name).delete  >> (Photos += pi )
      //Photos.insertOrUpdate(pi)
    }

    val action = for(
      a  <- deleteReq(photo);
      b <- innerSave(p);
      c <- insertReq(photo)
    ) yield(a,b,c) // update/insert counts
    db.run(action.transactionally)
  }

  // テーブル定義。必須。
  private class PhotosTable(tag: Tag) extends Table[PhotoInner](tag, "t_photo") {
    def album = column[String]("album")
    def name = column[String]("name")
    def etc = column[Int]("etc")
    def comment = column[String]("comment")
    def noDisp = column[Boolean]("no_disp")
    def pks = primaryKey("pk_photo", (album, name))
    def * = (album, name,etc,comment, noDisp) <>(PhotoInner.tupled, PhotoInner.unapply _)
  }
  private class PhotoRequestTable(tag: Tag) extends Table[PhotoRequest](tag, "t_photo_request") {
    def album = column[String]("album")
    def name = column[String]("name")
    def labelId = column[String]("label_id")
    def pks = primaryKey("pk_photo_request", (album, name, labelId))
    def * = (album, name, labelId) <>(PhotoRequest.tupled, PhotoRequest.unapply _)
  }
}
