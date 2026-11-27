package com.sageserpent.americium

trait CaseSupplyCycle {
  def isInitial: Boolean = 0 == numberOfPreviousCycles()
  def numberOfPreviousCycles(): Int
  def numberOfPreviousFailures(): Int
}

trait CasesLimitStrategy {
  def moreToDo(): Boolean
  def noteEmissionOfCase(): Unit
  def noteRejectionOfCase(): Unit
  def noteStarvation(): Unit
}

object CasesLimitStrategy {
  def timed(
      timeBudget: scala.concurrent.duration.FiniteDuration
  ): CasesLimitStrategy = new CasesLimitStrategy {
    private var deadline: Option[scala.concurrent.duration.Deadline] = None

    override def moreToDo(): Boolean = {
      if (deadline.isEmpty) {
        deadline = Some(timeBudget.fromNow)
      }
      deadline.exists(_.hasTimeLeft())
    }

    override def noteRejectionOfCase(): Unit = ()
    override def noteEmissionOfCase(): Unit  = ()
    override def noteStarvation(): Unit      = ()
  }

  def counted(
      maximumNumberOfCases: Int,
      maximumStarvationRatio: Double
  ): CasesLimitStrategy = {
    require(0 <= maximumNumberOfCases)
    require(0 <= maximumStarvationRatio)

    new CasesLimitStrategy {
      private var numberOfCasesEmitted: Int = 0
      private var starvationCount: Int      = 0

      override def moreToDo(): Boolean =
        maximumNumberOfCases > numberOfCasesEmitted &&
          starvationCount <= maximumNumberOfCases * maximumStarvationRatio

      override def noteRejectionOfCase(): Unit = {
        numberOfCasesEmitted -= 1
        starvationCount += 1
      }

      override def noteEmissionOfCase(): Unit = {
        numberOfCasesEmitted += 1
      }

      override def noteStarvation(): Unit = {
        starvationCount += 1
      }
    }
  }
}

class NoValidTrialsException
    extends RuntimeException("No valid trials were executed.")

trait Builder[-Element, +Collection] {
  def add(element: Element): Unit
  def build(): Collection
}
