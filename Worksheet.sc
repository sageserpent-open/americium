import com.sageserpent.americium.seqEnrichment._

import scala.collection.immutable.SortedSet

Seq(
  SortedSet.empty[Int],
  SortedSet(1),
  SortedSet(1073741826),
  SortedSet(21, 56, -3)
).zipN.force

Seq(List.empty[Int], List(1), List(1073741826), List(21, 56, -3)).zipN.force

Vector(1, 2, 2, 3, 3, 3, 2, 4, 4, 1).groupWhile(_ == _)

List(1, 2, 2, 3, 3, 3, 2, 4, 4, 1).groupWhile(_ == _)
