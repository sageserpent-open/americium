package com.sageserpent.americium

import com.sageserpent.americium.java.{Trials => JavaTrials}

object Trials {
  def constant[SomeCase](value: SomeCase): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](choices: SomeCase*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def choose[SomeCase](choices: Iterable[SomeCase]): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def alternate[SomeCase](alternatives: Trials[SomeCase]*): Trials[SomeCase] = {
    throw new NotImplementedError
  }

  def alternate[SomeCase](alternatives: Iterable[Trials[SomeCase]]): Trials[SomeCase] = {
    throw new NotImplementedError
  }
}

trait Trials[Case] extends JavaTrials[Case]
