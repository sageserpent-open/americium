import com.sageserpent.americium.Trials
import Trials.api

val threeUniqueIds = for {
  one   <- api.uniqueIds
  two   <- api.uniqueIds
  three <- api.uniqueIds
} yield (one, two, three)

threeUniqueIds.withLimit(10).supplyTo(println)

def recursiveUniqueIds: Trials[(Int, Int, Int)] =
  api.alternate(threeUniqueIds, api.delay(recursiveUniqueIds))

recursiveUniqueIds.withLimit(10).supplyTo(println)

def accumulatedRecursiveUniqueIds: Trials[List[(Int, Int, Int)]] =
  api.alternate(
    api.only(Nil),
    threeUniqueIds.flatMap(triple =>
      accumulatedRecursiveUniqueIds.map(triple :: _)
    )
  )

accumulatedRecursiveUniqueIds.withLimit(10).supplyTo(println)
