
import com.sageserpent.americium.Trials
import com.sageserpent.americium.java.{Trials => JavaTrials}

val trialsApi = Trials.api

val trials = trialsApi.choose(2, -4, 3)

val flatMappedTrials = trials flatMap ((integer =>
  trialsApi.only(1.1 * integer)))

flatMappedTrials.supplyTo(println)

val mappedTrials = trials map (_ * 2.5)

mappedTrials.supplyTo(println)

trialsApi.alternate(flatMappedTrials, mappedTrials).supplyTo(println)

val javaTrialsApi = JavaTrials.api

val javaTrials = javaTrialsApi.choose(2, -4, 3)

val flatMappedJavaTrials = javaTrials flatMap ((integer =>
  javaTrialsApi.only(1.1 * integer)))

flatMappedJavaTrials.supplyTo(println)

val mappedJavaTrials = javaTrials map (_ * 2.5)

mappedJavaTrials.supplyTo(println)

javaTrialsApi
  .alternate(flatMappedJavaTrials, mappedJavaTrials)
  .supplyTo(println)