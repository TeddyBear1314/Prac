import org.apache.spark.SparkConf

/**
  * Created by hz on 16/2/21.
  */
class first {
  val conf = new SparkConf().setMaster("local")
   val sc =new SparkContext(conf)
   val data = sc.parallelize(List(1,2,3,4))
    data.first()
}

