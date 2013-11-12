package com.sageserpent

import scala.util.Random

package object infrastructure {
  implicit def enrich(random: Random) = new RichRandom(random)
}
