import com.sageserpent.americium.seqEnrichment._

object Worksheet {
  Seq(Vector(), Vector(1), Vector(1073741826)).zipN[Vector, Int].force

  val groups = Vector(1, 2, 2).groupWhile((_, _) => false)
}