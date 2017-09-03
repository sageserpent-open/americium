import com.sageserpent.americium.seqEnrichment._

import scala.collection.immutable.SortedSet

object Worksheet {
  Seq(SortedSet.empty[Int], SortedSet(1), SortedSet(1073741826), SortedSet(21, 56, -3)).zipN.force

  Seq(List.empty[Int], List(1), List(1073741826), List(21, 56, -3)).zipN.force

  Vector(1, 2, 2).groupWhile((_, _) => false)

  List(1, 2, 2).groupWhile((_, _) => false)
}