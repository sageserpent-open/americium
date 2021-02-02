
import com.sageserpent.americium.Trials
import com.sageserpent.americium.java.TrialsApi

import java.util.function.{Consumer, Function => JavaFunction}

val trialsApi: TrialsApi = Trials

val trials: Trials[Int] = trialsApi.choose(2, -4, 3)

val flatMappedTrials: Trials[Double] = trials flatMap ((integer =>
  trialsApi.only(1.1 * integer)): JavaFunction[Int, Trials[Double]])

flatMappedTrials.supplyTo(println: Consumer[Double])

val flatMappedTrials2: Trials[Double] = trials flatMap (integer =>
  trialsApi.only(1.1 * integer))

flatMappedTrials2.supplyTo(println)

val mappedTrials
  : Trials[Double] = trials map ((_ * 2.5): JavaFunction[Int, Double])

mappedTrials.supplyTo(println: Consumer[Double])

val mappedTrials2: Trials[Double] = trials map (_ * 2.5)

mappedTrials2.supplyTo(println)