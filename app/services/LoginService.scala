package services

import com.google.inject.{Inject, ImplementedBy}
import models.UserDao
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@ImplementedBy(value = classOf[DBLoginService])
trait LoginService {
  def login(id:String, rawPassword:String):Boolean
}

class  DBLoginService @Inject() (encrypter: PasswordEncrypter, userDao: UserDao) extends LoginService {
  override def login(id: String, rawPassword: String): Boolean = {

    // Slick はすべて非同期で処理されるため、結果を取得する必要があるのならAwaitで取得する。
    // ここでwaitするのではなく、Action側で非同期にしてしまってもよい。
    val user = Await.result(userDao.find(id), 5 seconds)
    user.exists(_.password == encrypter.encrypt(rawPassword))

  }
}