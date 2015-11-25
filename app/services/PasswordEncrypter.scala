package services

import com.google.inject.ImplementedBy

/**
 */
@ImplementedBy(classOf[DefaultPasswordEncrypter])
trait PasswordEncrypter {
  def encrypt(raw:String):String
}
