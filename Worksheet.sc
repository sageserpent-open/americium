import com.sageserpent.infrastructure._

object Worksheet {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
  val unbounded = Finite(75)                      //> unbounded  : com.sageserpent.infrastructure.Finite[Int] = Finite(75)
  
  unbounded < NegativeInfinity                    //> res0: Boolean = false
  
  2 / 3.2                                         //> res1: Double(0.625) = 0.625
  
  Finite(3.2) >= Finite(2)                        //> res2: Boolean = true
  
  Finite(6) < PositiveInfinity                    //> res3: Boolean = true
  
  "Good morning, campers".map(x => x.toUpper)     //> res4: String = GOOD MORNING, CAMPERS
  
  2 / 3                                           //> res5: Int(0) = 0
  
  val x = 2                                       //> x  : Int = 2
  
  x / 3.0                                         //> res6: Double = 0.6666666666666666
  
}