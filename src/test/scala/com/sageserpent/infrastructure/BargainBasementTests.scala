package com.sageserpent.infrastructure

import junit.framework.TestCase
import org.junit.Test


class ThisShouldWorkToo extends TestCase {
  @Test
  def testThatIsnt(){
    System.out.println("At least this test gets run!")
    assert(1 != 0)
  }  
}


class BargainBasementTests extends TestCase {
  @Test
  def testIsSorted() = {
    assert(BargainBasement.isSorted(List.empty))

    assert(BargainBasement.isSorted(List(1)))

    assert(BargainBasement.isSorted(List(1, 2)))

    assert(!BargainBasement.isSorted(List(2, 1)))

    assert(BargainBasement.isSorted(List(1, 2, 3)))

    assert(BargainBasement.isSorted(List(1, 2, 2)))

    assert(BargainBasement.isSorted(List(1, 1, 2)))

    assert(BargainBasement.isSorted(List(1, 2, 2, 3)))

    assert(!BargainBasement.isSorted(List(1, 2, 1)))

    assert(BargainBasement.isSorted(List(1, 2, 2, 4)))

    assert(BargainBasement.isSorted(List(1, 2, 3, 4)))

    assert(!BargainBasement.isSorted(List(3, 2, 2, 4)))

    assert(!BargainBasement.isSorted(List(1, 2, 2, 0)))

    assert(!BargainBasement.isSorted(List(1, 2, 4, 0)))
  }
}