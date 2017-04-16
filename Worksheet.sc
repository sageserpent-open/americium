import com.sageserpent.americium.{NegativeInfinity, Finite, PositiveInfinity, BargainBasement, RichRandomTests}
import com.sageserpent.americium.randomEnrichment._
import com.sageserpent.americium.seqEnrichment._
import scala.collection.immutable.TreeMap
import scala.util.Random

object Worksheet {

  private val groupEverythingTogether: (Int, Int) => Boolean = {
    case (first, second) => true
  }

  private val groupNothingTogether: (Int, Int) => Boolean = {
    case (first, second) => false
  }

  private val group5To9Together: (Int, Int) => Boolean = {
    case (first, second) => first >= 5 && second <= 9
  }
  private val groupEqualTogether: (Int, Int) => Boolean = _ == _
  Seq.empty[Int].groupWhile(groupEverythingTogether)
  (0 to 0).groupWhile(groupEverythingTogether)
  (0 to 1).groupWhile(groupEverythingTogether)
  (0 to 2).groupWhile(groupEverythingTogether)
  (0 to 9).groupWhile(groupEverythingTogether)
  (5 to 20).groupWhile(groupEverythingTogether)
  (5 to 9).groupWhile(groupEverythingTogether)
  (7 to 8).groupWhile(groupEverythingTogether)
  (0 to 20).groupWhile(groupEverythingTogether)
  (Seq.empty[Int]).groupWhile(groupNothingTogether)
  (0 to 0).groupWhile(groupNothingTogether)
  (0 to 1).groupWhile(groupNothingTogether)
  (0 to 2).groupWhile(groupNothingTogether)
  (0 to 9).groupWhile(groupNothingTogether)
  (5 to 20).groupWhile(groupNothingTogether)
  (5 to 9).groupWhile(groupNothingTogether)
  (7 to 8).groupWhile(groupNothingTogether)
  (0 to 20).groupWhile(groupNothingTogether)
  (Seq.empty[Int]).groupWhile(group5To9Together)
  (0 to 0).groupWhile(group5To9Together)
  (0 to 1).groupWhile(group5To9Together)
  (0 to 2).groupWhile(group5To9Together)
  (0 to 9).groupWhile(group5To9Together)
  (5 to 20).groupWhile(group5To9Together)
  (5 to 9).groupWhile(group5To9Together)
  (7 to 8).groupWhile(group5To9Together)
  (0 to 20).groupWhile(group5To9Together)
  List(1, 0, 0, 2, 0, 0).groupWhile(groupEqualTogether)
  def foo() = 2
  val fooz = foo _
  def bar(z:Unit) = 3
  foo()
  fooz()
  bar()
  bar(())
  val baz = bar _
  baz()
  baz(())
  Stream(1) match {case Stream(x) => x}
  val stuff = 0 to 20
  stuff match {case Seq(a, b, x @ _*) => (a, b, x)}
  stuff splitAt(stuff length)
  val tm = TreeMap(0->0, 1->1, 2->2)
  tm.toList
  val rr = new Random(1)
  val bigStream = rr.pickAlternatelyFrom(List(0 to 4, 7 to 9, 22 to 25))

  bigStream.take(20).toList
  for (seed <- 0 to 100) yield {rr.nextDouble()}
  ((for (seed <- 0 to 1000000) yield {rr.nextInt(20)}).toList groupBy identity  mapValues (_.length) toSeq) sortBy (_._1)
  ((for (seed <- 0 to 1000000) yield {rr.chooseAnyNumberFromZeroToOneLessThan(20)}).toList groupBy identity  mapValues (_.length) toSeq) sortBy (_._1)
  ((for (seed <- 0 to 1000000) yield {rr.chooseAnyNumberFromOneTo(20)}).toList groupBy identity  mapValues (_.length) toSeq) sortBy (_._1)
  println("Welcome to the Scala worksheet")       
  (2, 'a')                                        
  2 -> 'a'                                        
  "%s".format(2)                                  
  val unbounded = Finite(75)                      
  unbounded < NegativeInfinity()                    
  2 / 3.2                                         
  Finite(3.2) >= PositiveInfinity()                 
  Finite(8) > PositiveInfinity()                    
  "Good morning, dampers".map(x => x.toUpper)     
  2 / 3                                           
  val x = 2                                       
  x / 3.0                                         
  1 until 10                                      
  1 to 10                                         
                                                  
  for (i <- 1 until 10) yield i * 2               
                                                  
  val random = new Random(10)                     
  val average = (Iterator.continually(random.chooseOneOf(0 to 7100)) take 1000).sum / 1000

  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  
                                                  
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  
                                                  
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  
                                                  
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  
                                                  
  BargainBasement.numberOfCombinations(18, 8)     
  BargainBasement.numberOfCombinations(18, 10)    
  BargainBasement.numberOfCombinations(18, 15)    
  BargainBasement.numberOfCombinations(20, 19)    
  BargainBasement.numberOfCombinations(20, 1)     
  BargainBasement.numberOfCombinations(20, 0)     
  BargainBasement.numberOfCombinations(20, 20)    
  BargainBasement.numberOfPermutations(5, 5)      
  BargainBasement.numberOfCombinations(98, 4)
  for (size <- 0 to 16)
    yield 0 to size map { BargainBasement.numberOfCombinations(size, _) } reduce (_ + _)
                                                  
                                                  
  val defaultMap = Map(1 -> 'a', 3 -> 'i')        
                                                  
  val modifiedMap = scala.collection.mutable.Map() withDefault (defaultMap.apply)
                                                  
  modifiedMap(3)                                  
  modifiedMap.keys                                
  modifiedMap += 4 -> 'k'                         
  modifiedMap.keys                                
  modifiedMap -= 3                                
  modifiedMap.keys                                
  modifiedMap(3)                                  
  modifiedMap -= 4                                
  modifiedMap.keys                                
  var modifiedMap2 = scala.collection.immutable.Map() withDefault (defaultMap.apply)
                                                  
  modifiedMap2(3)                                 
  modifiedMap2.keys                               
  modifiedMap2 += 4 -> 'k'
  modifiedMap2.keys                               
  modifiedMap2 -= 3
  modifiedMap2.keys                               
  modifiedMap2(3)                                 
  modifiedMap2 -= 4
  modifiedMap2.keys                               
  val things = 0 until 50                         
                                                  
                                                  
                                                  
  ((for (_ <- 0 until 10000)
    yield random.chooseOneOf(things)) groupBy identity map (pair => (pair._1, pair._2.size)) toSeq) sortWith (_._1 < _._1)
                                                  
                                                  
                                                  
                                                  
                                                  
                                                  
                                                  
                                                  
}