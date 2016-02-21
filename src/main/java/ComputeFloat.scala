
trait Logger{
  def log(str:String,key:Int=3):String
}
class CryptoLogger extends Logger{
  def log(str:String,key:Int):String={
    for(elem <- str)yield (if(key >= 0)(97+ (elem -97 +key)%26)else (97 + (elem-97+26+key)%26)).toChar
  }
}
object Test extends App{
  val plain ="huangzhi"
  println(plain)
}