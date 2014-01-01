import com.sageserpent.infrastructure._

import scala.util.Random

object Worksheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
  val unbounded = Finite(75)                      //> unbounded  : com.sageserpent.infrastructure.Finite[Int] = Finite(75)
  
  unbounded < NegativeInfinity                    //> res0: Boolean = false
  
  2 / 3.2                                         //> res1: Double(0.625) = 0.625
  
  Finite(3.2) >= PositiveInfinity                 //> res2: Boolean = false
  
  Finite(8) > PositiveInfinity                    //> res3: Boolean = false
  
  "Good morning, dampers".map(x => x.toUpper)     //> res4: String = GOOD MORNING, DAMPERS
  
  2 / 3                                           //> res5: Int(0) = 0
  
  val x = 2                                       //> x  : Int = 2
  
  x / 3.0                                         //> res6: Double = 0.6666666666666666
  
  1 until 10                                      //> res7: scala.collection.immutable.Range = Range(1, 2, 3, 4, 5, 6, 7, 8, 9)
  
  1 to 10                                         //> res8: scala.collection.immutable.Range.Inclusive = Range(1, 2, 3, 4, 5, 6, 7
                                                  //| , 8, 9, 10)
                                                  
  for (i <- 1 until 10) yield i * 2               //> res9: scala.collection.immutable.IndexedSeq[Int] = Vector(2, 4, 6, 8, 10, 12
                                                  //| , 14, 16, 18)
                                                  
  val random = new Random(10)                     //> random  : scala.util.Random = scala.util.Random@6e811c88
                                                  
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res10: scala.collection.immutable.Stream[Int] = Stream(0, 4, 3, 1, 5, 7, 6, 
                                                  //| 2, 8)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res11: scala.collection.immutable.Stream[Int] = Stream(1, 3, 8, 5, 6, 4, 0, 
                                                  //| 2, 7)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res12: scala.collection.immutable.Stream[Int] = Stream(4, 0, 7, 2, 1, 3, 8, 
                                                  //| 6, 5)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res13: scala.collection.immutable.Stream[Int] = Stream(5, 8, 1, 3, 4, 2, 7, 
                                                  //| 6, 0)
                                                  
  BargainBasement.numberOfCombinations(18, 8)     //> res14: Int = 43758
                                                  
  BargainBasement.numberOfCombinations(18, 10)    //> res15: Int = -34
                                                  
  BargainBasement.numberOfCombinations(18, 15)    //> res16: Int = 0
  
  val defaultMap = Map(1 -> 'a', 3 -> 'i')        //> defaultMap  : scala.collection.immutable.Map[Int,Char] = Map(1 -> a, 3 -> i
                                                  //| )
  
  val modifiedMap = scala.collection.mutable.Map() withDefault(defaultMap.apply)
                                                  //> modifiedMap  : scala.collection.mutable.Map[Int,Char] = Map()
                                                  
  modifiedMap(3)                                  //> res17: Char = i
  
  modifiedMap.keys                                //> res18: Iterable[Int] = Set()
  
  modifiedMap += 4 -> 'k'                         //> res19: Worksheet.modifiedMap.type = Map(4 -> k)
  
  modifiedMap.keys                                //> res20: Iterable[Int] = Set(4)
  
  modifiedMap -= 3                                //> res21: Worksheet.modifiedMap.type = Map(4 -> k)
  
  modifiedMap.keys                                //> res22: Iterable[Int] = Set(4)
  
  modifiedMap(3)                                  //> res23: Char = i
  
  modifiedMap -= 4                                //> res24: Worksheet.modifiedMap.type = Map()
  
  modifiedMap.keys                                //> res25: Iterable[Int] = Set()
  
  
  var modifiedMap2 = scala.collection.immutable.Map() withDefault(defaultMap.apply)
                                                  //> modifiedMap2  : scala.collection.immutable.Map[Int,Char] = Map()
  
  modifiedMap2(3)                                 //> res26: Char = i
  
  modifiedMap2.keys                               //> res27: Iterable[Int] = Set()
  
  modifiedMap2 += 4 -> 'k'
  
  modifiedMap2.keys                               //> res28: Iterable[Int] = Set(4)
  
  modifiedMap2 -= 3
  
  modifiedMap2.keys                               //> res29: Iterable[Int] = Set(4)
  
  modifiedMap2(3)                                 //> res30: Char = i

  modifiedMap2 -= 4
  
  modifiedMap2.keys                               //> res31: Iterable[Int] = Set()

}