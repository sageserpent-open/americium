import com.sageserpent.americium.Trials
import Trials.api
import scala.collection.immutable.SortedMap
import cats.instances.map._
import cats.kernel.Monoid
import cats.syntax.monoid

sealed trait Tree {
  def maximumDepth: Int

  def depthFrequencies: SortedMap[Int, Int]
}

case class Leaf(value: Int) extends Tree {
  override def maximumDepth: Int = 1

  override def depthFrequencies: SortedMap[Int, Int] = SortedMap(1 -> 1)
}

case class Branching(subtrees: List[Tree]) extends Tree {
  require(subtrees.nonEmpty)

  override def maximumDepth: Int =
    1 + subtrees.map(_.maximumDepth).max

  override def depthFrequencies: SortedMap[Int, Int] = subtrees
    .map(_.depthFrequencies)
    .reduce[SortedMap[Int, Int]](Monoid[SortedMap[Int, Int]].combine)
    .map { case (value, frequency) => (1 + value) -> frequency }
}

def trees: Trials[Tree] = api.alternate(
  api.uniqueIds.map(Leaf.apply),
  api
    .integers(1, 5)
    .flatMap(numberOfSubtrees =>
      trees.listsOfSize(numberOfSubtrees).map(Branching.apply)
    )
)

trees.withLimit(10).supplyTo { tree =>
  println(
    s"Tree: $tree,\nmaximum depth: ${tree.maximumDepth} with depth frequencies: ${tree.depthFrequencies}\n"
  )
}

def statelyTrees: Trials[Tree] = api.complexities.flatMap(complexity =>
  api.alternateWithWeights(
    complexity -> api.uniqueIds.map(Leaf.apply),
    2 -> api
      .integers(1, 5)
      .flatMap(numberOfSubtrees =>
        statelyTrees.listsOfSize(numberOfSubtrees).map(Branching.apply)
      )
  )
)

statelyTrees.withLimit(10).supplyTo { tree =>
  println(
    s"stately tree: $tree,\nmaximum depth: ${tree.maximumDepth} with depth frequencies: ${tree.depthFrequencies}\n"
  )
}
