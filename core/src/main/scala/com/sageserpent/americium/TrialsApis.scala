package com.sageserpent.americium
import com.sageserpent.americium.TrialsApiImplementation as ScalaTrialsApiImplementation
import com.sageserpent.americium.java.TrialsApiImplementation as JavaTrialsApiImplementation

object TrialsApis {
  val javaApi = new JavaTrialsApiImplementation {
    override def scalaApi: ScalaTrialsApiImplementation =
      TrialsApis.scalaApi
  }

  val scalaApi = new ScalaTrialsApiImplementation
}
