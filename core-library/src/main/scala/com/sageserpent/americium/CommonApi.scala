package com.sageserpent.americium
import cats.free.Free.pure
import com.sageserpent.americium.generation.GenerationOperation

trait CommonApi {
  def only[Case](onlyCase: Case): TrialsImplementation[Case] =
    TrialsImplementation(pure[GenerationOperation, Case](onlyCase))
}
