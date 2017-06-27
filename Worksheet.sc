import com.sageserpent.americium.seqEnrichment._

import scala.collection.immutable.SortedSet

object Worksheet {
  Seq(SortedSet.empty[Int], SortedSet(1), SortedSet(1073741826), SortedSet(21, 56, -3)).zipN.force

  val groups = Vector(1, 2, 2).groupWhile((_, _) => false)
}