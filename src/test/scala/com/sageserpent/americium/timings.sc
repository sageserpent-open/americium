import com.sageserpent.americium.Trials

import java.time.Instant
import scala.annotation.tailrec

val api = Trials.api

val choices = api.choose(0 until 100)

def iterate[X](count: Long, initial: X, step: X => X): X = {
  @tailrec
  def innerIterate(count: Long, initial: X): X =
    if (0 >= count) initial else innerIterate(count - 1, step(initial))

  innerIterate(count, initial)
}

def testDrive(count: Long): Unit =
  iterate[Trials[Int]](count, api.only(0), _.flatMap(_ => choices))
    .withLimit(1, Int.MaxValue)
    .supplyTo(_ => {})

def timeOf(thing: => Unit): Long = {
  val startTime = Instant.now()

  val _ = thing

  val endTime = Instant.now()

  endTime.toEpochMilli - startTime.toEpochMilli
}

val numberOfRepeats = 5

def meanTimeOf(thing: => Unit): Double =
  Seq.fill(numberOfRepeats)(timeOf(thing)).sum.toDouble / numberOfRepeats

meanTimeOf(testDrive(1))

meanTimeOf(testDrive(1L))
meanTimeOf(testDrive(10L))
meanTimeOf(testDrive(100L))
meanTimeOf(testDrive(1000L))
meanTimeOf(testDrive(2000L))
meanTimeOf(testDrive(2000L))
meanTimeOf(testDrive(4000L))
meanTimeOf(testDrive(5000L))
meanTimeOf(testDrive(6000L))
meanTimeOf(testDrive(7000L))
meanTimeOf(testDrive(8000L))
meanTimeOf(testDrive(9000L))
meanTimeOf(testDrive(10000L))
meanTimeOf(testDrive(20000L))
meanTimeOf(testDrive(30000L))
meanTimeOf(testDrive(40000L))
