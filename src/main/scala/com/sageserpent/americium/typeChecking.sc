
import com.sageserpent.americium.Trials
import com.sageserpent.americium.java.TrialsApi

import java.util.function.{Consumer, Function => JavaFunction}

val trialsApi: TrialsApi = Trials

val trials: Trials[Int] = trialsApi.choose(2, -4, 3)

val flatMappedTrials: Trials[Int] = trials flatMap ((integer =>
  trialsApi.only(integer)): JavaFunction[Int, Trials[Int]])

val mappedTrials
  : Trials[Double] = trials map ((_ * 2.5): JavaFunction[Int, Double])

mappedTrials.supplyTo(println: Consumer[Double])