package com.sageserpent.americium.java

import com.sageserpent.americium.TrialsApis as ScalaTrialsApis

object TrialsApis {
  val javaApi: TrialsApi = new TrialsApiImplementation {
    override def scalaApi = ScalaTrialsApis.scalaApi
  }
}
