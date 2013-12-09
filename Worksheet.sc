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
                                                  
  val random = new Random(10)                     //> random  : scala.util.Random = scala.util.Random@1e6f7cb
                                                  
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> java.lang.RuntimeException
                                                  //| 	at com.sageserpent.infrastructure.RichRandom$InteriorNode$3.recurseOnLes
                                                  //| serSubtree$1(RichRandom.scala:89)
                                                  //| 	at com.sageserpent.infrastructure.RichRandom$InteriorNode$3.addNewItemIn
                                                  //| TheVacantSlotAtIndex(RichRandom.scala:129)
                                                  //| 	at com.sageserpent.infrastructure.RichRandom$BinaryTreeNode$1.addNewItem
                                                  //| InTheVacantSlotAtIndex(RichRandom.scala:44)
                                                  //| 	at com.sageserpent.infrastructure.RichRandom.com$sageserpent$infrastruct
                                                  //| ure$RichRandom$$chooseAndRecordUniqueItems$1(RichRandom.scala:171)
                                                  //| 	at com.sageserpent.infrastructure.RichRandom$$anonfun$com$sageserpent$in
                                                  //| frastructure$RichRandom$$chooseAndRecordUniqueItems$1$1.apply(RichRandom.sca
                                                  //| la:175)
                                                  //| 	at com.sageserpent.infrastructure.RichRandom$$anonfun$com$sageserpent$in
                                                  //| frastructure$RichRandom$$chooseAndRecordUniqueItems$1$1.apply(RichRandom.sca
                                                  //| la:175)
                                                  //| 	at scala.collection.immutable.Stream$Cons.tail(Stream.scala:1085)
                                                  //| 	at scala.collection.immutab
                                                  //| Output exceeds cutoff limit.
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
}