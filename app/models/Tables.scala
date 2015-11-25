package models

import com.google.inject.{Singleton, Inject}
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}
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
  private class UsersTable(tag: Tag) extends Table[User](tag, "T_USER") {
    // IDは大文字である必要がある。
    def id = column[String]("ID", O.PrimaryKey)

    def password = column[String]("PASSWORD")
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
  private class LabelsTable(tag: Tag) extends Table[Label](tag, "T_LABEL") {
    // IDは大文字である必要がある。
    def id = column[String]("ID", O.PrimaryKey)

    def name = column[String]("NAME")
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
      .join(PhotoRequests).on {case (a,b) => a.album === b.album && a.name === b.name}

    db.run(q.result).map{
      case Nil => None
      case x::xs => {
        val p = x._1
        val reqs = x._2 :: xs.map(_._2)
        Some(Photo(p.album, p.name, p.etc, p.comment, p.noDisp, reqs))
      }
    }
  }

  def findByLabel(album:String, label:String) = {
    val query =
      for (p <- Photos if (p.album === album)
      ) yield (p)
    // subquery exisits.
    db.run(query.filter(p => p.reqs.filter(_.labelId === label).exists).result)
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
    db.run(deleteReq(photo).andThen(Photos.insertOrUpdate(p))
      .andThen(insertReq(photo))
    )
  }

  // テーブル定義。必須。
  private class PhotosTable(tag: Tag) extends Table[PhotoInner](tag, "T_PHOTO") {
    // IDは大文字である必要がある。
    def album = column[String]("ALBUM", O.PrimaryKey)
    def name = column[String]("NAME", O.PrimaryKey)
    def etc = column[Int]("ETC")
    def comment = column[String]("COMMENT")
    def noDisp = column[Boolean]("NO_DISP")
    def reqs = foreignKey("FK_PHOTO_REQ", (album, name),PhotoRequests)(l => (l.album, l.name))
    def * = (album, name,etc,comment, noDisp) <>(PhotoInner.tupled, PhotoInner.unapply _)
  }
  private class PhotoRequestTable(tag: Tag) extends Table[PhotoRequest](tag, "T_PHOTO_REQUEST") {
    // IDは大文字である必要がある。
    def album = column[String]("ALBUM", O.PrimaryKey)
    def name = column[String]("NAME", O.PrimaryKey)
    def labelId = column[String]("LABEL_ID", O.PrimaryKey)
    def * = (album, name, labelId) <>(PhotoRequest.tupled, PhotoRequest.unapply _)
  }
}
