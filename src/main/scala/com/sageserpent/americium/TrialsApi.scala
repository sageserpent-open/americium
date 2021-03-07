package com.sageserpent.americium

trait TrialsApi {
  def only[Case](onlyCase: Case): Trials[Case]

  def choose[Case](firstChoice: Case,
                   secondChoice: Case,
                   otherChoices: Case*): Trials[Case]

  def choose[Case](choices: Iterable[Case]): Trials[Case]

  def alternate[Case](firstAlternative: Trials[Case],
                      secondAlternative: Trials[Case],
                      otherAlternatives: Trials[Case]*): Trials[Case]

  def alternate[Case](alternatives: Iterable[Trials[Case]]): Trials[Case]

  def stream[Case](factory: Long => Case): Trials[Case]
}
