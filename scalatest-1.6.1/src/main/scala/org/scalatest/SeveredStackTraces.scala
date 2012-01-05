package org.scalatest

/**
 * Trait that causes <code>StackDepth</code> exceptions thrown by a running test (such as <code>TestFailedException</code>s) to have
 * the exception's stack trace severed at the stack depth. Because the stack depth indicates the exact line of code that caused
 * the exception to be thrown, the severed stack trace will show that offending line of code on top. This can make the line
 * of test code that discovered a problem to be more easily found in IDEs and tools that don't make use of
 * ScalaTest's <code>StackDepth</code> exceptions directly.
 *
 * @author Bill Venners
 */
trait SeveredStackTraces extends AbstractSuite { this: Suite =>

  abstract override def withFixture(test: NoArgTest) {
    try {
      super.withFixture(test)
    }
    catch {
      case e: StackDepth =>
        throw e.severedAtStackDepth
    }
  }
}
