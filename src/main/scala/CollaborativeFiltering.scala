import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by hz on 16/2/21.
  */
class CollaborativeFiltering extends App{
  val conf = new SparkConf().setAppName("CollaborativeFiltering")
  val sc = new SparkContext(conf)

  val rawUserArtistData = sc.textFile("hdfs://user/ds/user_artist_data.txt")
  rawUserArtistData.map(_.split(" ")(0).toDouble).stats()
  rawUserArtistData.map(_.split(" ")(1).toDouble).stats()


}
