package com.sageserpent.americium
import cats.free.Free.pure
import com.sageserpent.americium.generation.{Factory, GenerationOperation}
import com.sageserpent.americium.java.CaseFactory

trait CommonApi {

  def only[Case](onlyCase: Case): TrialsImplementation[Case] =
    TrialsImplementation(pure[GenerationOperation, Case](onlyCase))

  def stream[Case](
      caseFactory: CaseFactory[Case]
  ): TrialsImplementation[Case] = new TrialsImplementation(
    Factory(new CaseFactory[Case] {
      override def apply(input: Long): Case = {
        require(lowerBoundInput() <= input)
        require(upperBoundInput() >= input)
        caseFactory(input)
      }
      override def lowerBoundInput(): Long = caseFactory.lowerBoundInput()
      override def upperBoundInput(): Long = caseFactory.upperBoundInput()
      override def maximallyShrunkInput(): Long =
        caseFactory.maximallyShrunkInput()
    })
  )
}
