import com.sageserpent.infrastructure._
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
  BargainBasement.groupWhile(Seq.empty[Int], groupEverythingTogether)
  BargainBasement.groupWhile(0 to 0, groupEverythingTogether)
  BargainBasement.groupWhile(0 to 1, groupEverythingTogether)
  BargainBasement.groupWhile(0 to 2, groupEverythingTogether)
  BargainBasement.groupWhile(0 to 9, groupEverythingTogether)
  BargainBasement.groupWhile(5 to 20, groupEverythingTogether)
  BargainBasement.groupWhile(5 to 9, groupEverythingTogether)
  BargainBasement.groupWhile(7 to 8, groupEverythingTogether)
  BargainBasement.groupWhile(0 to 20, groupEverythingTogether)

  BargainBasement.groupWhile(Seq.empty[Int], groupNothingTogether)
  BargainBasement.groupWhile(0 to 0, groupNothingTogether)
  BargainBasement.groupWhile(0 to 1, groupNothingTogether)
  BargainBasement.groupWhile(0 to 2, groupNothingTogether)
  BargainBasement.groupWhile(0 to 9, groupNothingTogether)
  BargainBasement.groupWhile(5 to 20, groupNothingTogether)
  BargainBasement.groupWhile(5 to 9, groupNothingTogether)
  BargainBasement.groupWhile(7 to 8, groupNothingTogether)
  BargainBasement.groupWhile(0 to 20, groupNothingTogether)

  BargainBasement.groupWhile(Seq.empty[Int], group5To9Together)
  BargainBasement.groupWhile(0 to 0, group5To9Together)
  BargainBasement.groupWhile(0 to 1, group5To9Together)
  BargainBasement.groupWhile(0 to 2, group5To9Together)
  BargainBasement.groupWhile(0 to 9, group5To9Together)
  BargainBasement.groupWhile(5 to 20, group5To9Together)
  BargainBasement.groupWhile(5 to 9, group5To9Together)
  BargainBasement.groupWhile(7 to 8, group5To9Together)
  BargainBasement.groupWhile(0 to 20, group5To9Together)
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
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  (2, 'a')                                        //> res0: (Int, Char) = (2,a)
  2 -> 'a'                                        //> res1: (Int, Char) = (2,a)
  "%s".format(2)                                  //> res2: String = 2
  val unbounded = Finite(75)                      //> unbounded  : com.sageserpent.infrastructure.Finite[Int] = Finite(75)
  unbounded < NegativeInfinity()                    //> res3: Boolean = false
  2 / 3.2                                         //> res4: Double(0.625) = 0.625
  Finite(3.2) >= PositiveInfinity()                 //> res5: Boolean = false
  Finite(8) > PositiveInfinity()                    //> res6: Boolean = false
  "Good morning, dampers".map(x => x.toUpper)     //> res7: String = GOOD MORNING, DAMPERS
  2 / 3                                           //> res8: Int(0) = 0
  val x = 2                                       //> x  : Int = 2
  x / 3.0                                         //> res9: Double = 0.6666666666666666
  1 until 10                                      //> res10: scala.collection.immutable.Range = Range(1, 2, 3, 4, 5, 6, 7, 8, 9)
  1 to 10                                         //> res11: scala.collection.immutable.Range.Inclusive = Range(1, 2, 3, 4, 5, 6,
                                                  //| 7, 8, 9, 10)
  for (i <- 1 until 10) yield i * 2               //> res12: scala.collection.immutable.IndexedSeq[Int] = Vector(2, 4, 6, 8, 10, 1
                                                  //| 2, 14, 16, 18)
  val random = new Random(10)                     //> random  : scala.util.Random = scala.util.Random@1e0bf98
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res13: scala.collection.immutable.Stream[Int] = Stream(0, 4, 3, 1, 5, 7, 6, 
                                                  //| 2, 8)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res14: scala.collection.immutable.Stream[Int] = Stream(1, 3, 8, 5, 6, 4, 0, 
                                                  //| 2, 7)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res15: scala.collection.immutable.Stream[Int] = Stream(4, 0, 7, 2, 1, 3, 8, 
                                                  //| 6, 5)
  random.buildRandomSequenceOfDistinctIntegersFromZeroToOneLessThan(9).force
                                                  //> res16: scala.collection.immutable.Stream[Int] = Stream(5, 8, 1, 3, 4, 2, 7, 
                                                  //| 6, 0)
  BargainBasement.numberOfCombinations(18, 8)     //> res17: Int = 43758
  BargainBasement.numberOfCombinations(18, 10)    //> res18: Int = 43758
  BargainBasement.numberOfCombinations(18, 15)    //> res19: Int = 816
  BargainBasement.numberOfCombinations(20, 19)    //> res20: Int = 20
  BargainBasement.numberOfCombinations(20, 1)     //> res21: Int = 20
  BargainBasement.numberOfCombinations(20, 0)     //> res22: Int = 1
  BargainBasement.numberOfCombinations(20, 20)    //> res23: Int = 1
  BargainBasement.numberOfPermutations(5, 5)      //> res24: Int = 120
  for (size <- 0 to 16)
    yield 0 to size map { BargainBasement.numberOfCombinations(size, _) } reduce (_ + _)
                                                  //> res25: scala.collection.immutable.IndexedSeq[Int] = Vector(1, 2, 4, 8, 16, 
                                                  //| 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536)
  val defaultMap = Map(1 -> 'a', 3 -> 'i')        //> defaultMap  : scala.collection.immutable.Map[Int,Char] = Map(1 -> a, 3 -> i
                                                  //| )
  val modifiedMap = scala.collection.mutable.Map() withDefault (defaultMap.apply)
                                                  //> modifiedMap  : scala.collection.mutable.Map[Int,Char] = Map()
  modifiedMap(3)                                  //> res26: Char = i
  modifiedMap.keys                                //> res27: Iterable[Int] = Set()
  modifiedMap += 4 -> 'k'                         //> res28: Worksheet.modifiedMap.type = Map(4 -> k)
  modifiedMap.keys                                //> res29: Iterable[Int] = Set(4)
  modifiedMap -= 3                                //> res30: Worksheet.modifiedMap.type = Map(4 -> k)
  modifiedMap.keys                                //> res31: Iterable[Int] = Set(4)
  modifiedMap(3)                                  //> res32: Char = i
  modifiedMap -= 4                                //> res33: Worksheet.modifiedMap.type = Map()
  modifiedMap.keys                                //> res34: Iterable[Int] = Set()
  var modifiedMap2 = scala.collection.immutable.Map() withDefault (defaultMap.apply)
                                                  //> modifiedMap2  : scala.collection.immutable.Map[Int,Char] = Map()
  modifiedMap2(3)                                 //> res35: Char = i
  modifiedMap2.keys                               //> res36: Iterable[Int] = Set()
  modifiedMap2 += 4 -> 'k'
  modifiedMap2.keys                               //> res37: Iterable[Int] = Set(4)
  modifiedMap2 -= 3
  modifiedMap2.keys                               //> res38: Iterable[Int] = Set(4)
  modifiedMap2(3)                                 //> res39: Char = i
  modifiedMap2 -= 4
  modifiedMap2.keys                               //> res40: Iterable[Int] = Set()
  val things = 0 until 50                         //> things  : scala.collection.immutable.Range = Range(0, 1, 2, 3, 4, 5, 6, 7,
                                                  //| 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 2
                                                  //| 7, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 
                                                  //| 46, 47, 48, 49)
  ((for (_ <- 0 until 10000)
    yield random.chooseOneOf(things)) groupBy identity map (pair => (pair._1, pair._2.size)) toSeq) sortWith (_._1 < _._1)
                                                  //> res41: Seq[(Int, Int)] = ArrayBuffer((0,187), (1,198), (2,197), (3,201), (4
                                                  //| ,193), (5,207), (6,190), (7,215), (8,201), (9,198), (10,205), (11,212), (12
                                                  //| ,199), (13,200), (14,226), (15,206), (16,200), (17,172), (18,189), (19,193)
                                                  //| , (20,188), (21,216), (22,200), (23,197), (24,217), (25,208), (26,201), (27
                                                  //| ,184), (28,199), (29,186), (30,201), (31,207), (32,189), (33,198), (34,184)
                                                  //| , (35,207), (36,217), (37,209), (38,197), (39,188), (40,192), (41,188), (42
                                                  //| ,218), (43,197), (44,245), (45,205), (46,196), (47,180), (48,185), (49,212)
                                                  //| )
  new RichRandomTests() commonTestStructureForTestingAlternatePickingFromSequences(println)
                                                  //> List()
                                                  //| List(List())
                                                  //| List(List(), List(74))
                                                  //| List(List(), List(96), List(37, 40))
                                                  //| List(List(), List(1), List(15, 19), List(21, 25, 29))
                                                  //| List(List(), List(87), List(97, 102), List(26, 31, 36), List(0, 5, 10, 15))
                                                  //| 
                                                  //| List(List(), List(32), List(61, 67), List(65, 71, 77), List(52, 58, 64, 70)
                                                  //| , List(99, 105, 111, 117, 123))
                                                  //| List(List(), List(36), List(55, 62), List(19, 26, 33), List(3, 10, 17, 24),
                                                  //|  List(12, 19, 26, 33, 40), List(77, 84, 91, 98, 105, 112))
                                                  //| List(List(), List(63), List(85, 93), List(18, 26, 34), List(93, 101, 109, 1
                                                  //| 17), List(96, 104, 112, 120, 128), List(89, 97, 105, 113, 121, 129), List(8
                                                  //| , 16, 24, 32, 40, 48, 56))
                                                  //| List(List(), List(66), List(72, 81), List(0, 9, 18), List(56, 65, 74, 83), 
                                                  //| List(29, 38, 47, 56, 65), List(6, 15, 24, 33, 42, 51), List(50, 59, 68, 77,
                                                  //|  86, 95, 104), List(18, 27, 36, 45, 54, 63, 72, 81))
                                                  //| List(List(), List(29), List(54, 64), List(0, 10, 20), List(86,
                                                  //| Output exceeds cutoff limit.
}