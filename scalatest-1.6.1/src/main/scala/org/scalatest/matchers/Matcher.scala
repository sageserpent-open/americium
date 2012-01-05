/*
 * Copyright 2001-2008 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.matchers

import org.scalatest._

/**
 * Trait extended by objects that can match a value of the specified type. The value to match is
 * passed to the matcher's <code>apply</code> method. The result is a <code>MatchResult</code>.
 * A matcher is, therefore, a function from the specified type, <code>T</code>, to a <code>MatchResult</code>.
 * <p></p> <!-- needed otherwise the heading below shows up in the wrong place. dumb scaladoc algo -->
 *
 * <h2>Creating custom matchers</h2>
 * 
 * <p>
 * <em>Note: We are planning on adding some new matchers to ScalaTest in a future release, and would like your feedback.
 * Please let us know if you have felt the need for a matcher ScalaTest doesn't yet provide, whether or
 * not you wrote a custom matcher for it. Please email your feedback to bill AT artima.com.</em>
 * </p>
 *
 * <p>
 * If none of the built-in matcher syntax satisfies a particular need you have, you can create
 * custom <code>Matcher</code>s that allow
 * you to place your own syntax directly after <code>should</code> or <code>must</code>. For example, class <code>java.io.File</code> has a method <code>exists</code>, which
 * indicates whether a file of a certain path and name exists. Because the <code>exists</code> method takes no parameters and returns <code>Boolean</code>,
 * you can call it using <code>be</code> with a symbol or <code>BePropertyMatcher</code>, yielding assertions like:
 * </p>
 * 
 * <pre class="stHighlight">
 * file should be ('exists)  // using a symbol
 * file should be (inExistance)   // using a BePropertyMatcher
 * </pre>
 * 
 * <p>
 * Although these expressions will achieve your goal of throwing a <code>TestFailedException</code> if the file does not exist, they don't produce
 * the most readable code because the English is either incorrect or awkward. In this case, you might want to create a
 * custom <code>Matcher[java.io.File]</code>
 * named <code>exist</code>, which you could then use to write expressions like:
 * </p>
 *
 * <pre class="stHighlight">
 * // using a plain-old Matcher
 * file should exist
 * file should not (exist)
 * file should (exist and have ('name ("temp.txt")))
 * </pre>
 * 
 * <p>
 * One good way to organize custom matchers is to place them inside one or more
 * traits that you can then mix into the suites or specs that need them. Here's an example:
 * </p>
 *
 * <pre class="stHighlight">
 * trait CustomMatchers {
 * 
 *   class FileExistsMatcher extends Matcher[java.io.File] {
 * 
 *     def apply(left: java.io.File) = {
 * 
 *       val fileOrDir = if (left.isFile) "file" else "directory"
 * 
 *       val failureMessageSuffix = 
 *         fileOrDir + " named " + left.getName + " did not exist"
 * 
 *       val negatedFailureMessageSuffix = 
 *         fileOrDir + " named " + left.getName + " existed"
 * 
 *       MatchResult(
 *         left.exists,
 *         "The " + failureMessageSuffix,
 *         "The " + negatedFailureMessageSuffix,
 *         "the " + failureMessageSuffix,
 *         "the " + negatedFailureMessageSuffix
 *       )
 *     }
 *   }
 * 
 *   val exist = new FileExistsMatcher
 * }
 *
 * // Make them easy to import with:
 * // import CustomMatchers._
 * object CustomMatchers extends CustomMatchers
 * </pre>
 * 
 * <p>
 * Note: the <code>CustomMatchers</code> companion object exists to make it easy to bring the
 * matchers defined in this trait into scope via importing, instead of mixing in the trait. The ability
 * to import them is useful, for example, when you want to use the matchers defined in a trait in the Scala interpreter console.
 * </p>
 *
 * <p>
 * This trait contains one matcher class, <code>FileExistsMatcher</code>, and a <code>val</code> named <code>exist</code> that refers to
 * an instance of <code>FileExistsMatcher</code>. Because the class extends <code>Matcher[java.io.File]</code>,
 * the compiler will only allow it be used to match against instances of <code>java.io.File</code>. A matcher must declare an
 * <code>apply</code> method that takes the type decared in <code>Matcher</code>'s type parameter, in this case <code>java.io.File</code>.
 * The apply method will return a <code>MatchResult</code> whose <code>matches</code> field will indicate whether the match succeeded.
 * The <code>failureMessage</code> field will provide a programmer-friendly error message indicating, in the event of a match failure, what caused
 * the match to fail. 
 * </p>
 *
 * <p>
 * The <code>FileExistsMatcher</code> matcher in this example determines success by calling <code>exists</code> on the passed <code>java.io.File</code>. It
 * does this in the first argument passed to the <code>MatchResult</code> factory method:
 * </p>
 *
 * <pre class="stHighlight">
 *         left.exists,
 * </pre>
 *
 * <p>
 * In other words, if the file exists, this matcher matches.
 * The next argument to <code>MatchResult</code>'s factory method produces the failure message string:
 * </p>
 *
 * <pre class="stHighlight">
 *         "The " + failureMessageSuffix,
 * </pre>
 *
 * <p>
 * If the passed <code>java.io.File</code> is a file (not a directory) and has the name <code>temp.txt</code>, for example, the failure
 * message would be:
 * </p>
 *
 * <pre>
 * The file named temp.txt did not exist
 * </pre>
 *
 * <p>
 * For more information on the fields in a <code>MatchResult</code>, including the subsequent three fields that follow the failure message,
 * please see the documentation for <a href="MatchResult.html"><code>MatchResult</code></a>.
 * </p>
 *
 * <p>
 * Given the <code>CustomMatchers</code> trait as defined above, you can use the <code>exist</code> syntax in any suite or spec in
 * which you mix in the trait:
 * </p>
 *
 * <pre class="stHighlight">
 * class ExampleSpec extends Spec with ShouldMatchers with CustomMatchers {
 * 
 *   describe("A temp file") {
 * 
 *     it("should be created and deleted") {
 * 
 *       val tempFile = java.io.File.createTempFile("delete", "me")
 * 
 *       try {
 *         // At this point the temp file should exist
 *         tempFile should exist
 *       }
 *       finally {
 *         tempFile.delete()
 *       }
 * 
 *       // At this point it should not exist
 *       tempFile should not (exist)
 *     }
 *   }
 * }
 * </pre>
 *  
 * <p>
 * Note that when you use custom <code>Matcher</code>s, you will need to put parentheses around the custom matcher when if follows <code>not</code>,
 * as shown in the last assertion above: <code>tempFile should not (exist)</code>.
 * </p>
 *
 * <a name="otherways"></a><h2>Other ways to create matchers</h2>
 *
 * <p>
 * There are other ways to create new matchers besides defining one as shown above. For example, you would normally check to ensure
 * an option is defined like this:
 * </p>
 *
 * <pre class="stHighlight">
 * Some("hi") should be ('defined)
 * </pre>
 *
 * <p>
 * If you wanted to get rid of the tick mark, you could simply define <code>defined</code> like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val defined = 'defined
 * </pre>
 *
 * <p>
 * Now you can check that an option is defined without the tick mark:
 * </p>
 *
 * <pre class="stHighlight">
 * Some("hi") should be (defined)
 * </pre>
 *
 * <p>
 * Perhaps after using that for a while, you realize you're tired of typing the parentheses. You could
 * get rid of them with another one-liner:
 * </p>
 *
 * <pre class="stHighlight">
 * val beDefined = be (defined)
 * </pre>
 *
 * <p>
 * Now you can check that an option is defined without the tick mark or the parentheses:
 * </p>
 *
 * <pre class="stHighlight">
 * Some("hi") should beDefined
 * </pre>
 *
 * <p>
 * You can also use ScalaTest matchers' logical operators to combine existing matchers into new ones, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val beWithinTolerance = be >= 0 and be <= 10
 * </pre>
 *
 * <p>
 * Now you could check that a number is within the tolerance (in this case, between 0 and 10, inclusive), like this:
 * </p>
 *
 * <pre class="stHighlight">
 * num should beWithinTolerance
 * </pre>
 *
 * <p>
 * When defining a full blown matcher, one shorthand is to use one of the factory methods in <code>Matcher</code>'s companion
 * object. For example, instead of writing this:
 * </p>
 *
 * <pre class="stHighlight">
 * val beOdd =
 *   new Matcher[Int] {
 *     def apply(left: Int) =
 *       MatchResult(
 *         left % 2 == 1,
 *         left + " was not odd",
 *         left + " was odd"
 *       )
 *   }
 * </pre>
 *
 * <p>
 * You could alternately write this:
 * </p>
 *
 * <pre class="stHighlight">
 * val beOdd =
 *   Matcher { (left: Int) =>
 *     MatchResult(
 *       left % 2 == 1,
 *       left + " was not odd",
 *       left + " was odd"
 *     )
 *   }
 * </pre>
 *
 * <p>
 * Either way you define the <code>beOdd</code> matcher, you could use it like this:
 * </p>
 *
 * <pre class="stHighlight">
 * 3 should beOdd
 * 4 should not (beOdd)
 * </pre>
 *
 * <p>
 * You can also compose matchers. If for some odd reason, you wanted a <code>Matcher[String]</code> that 
 * checked whether a string, when converted to an <code>Int</code>,
 * was odd, you could make one by composing <code>beOdd</code> with
 * a function that converts a string to an <code>Int</code>, like this:
 * </p>
 *
 * <pre class="stHighlight">
 * val beOddAsInt = beOdd compose { (s: String) => s.toInt }
 * </pre>
 *
 * <p>
 * Now you have a <code>Matcher[String]</code> whose <code>apply</code> method first
 * invokes the converter function to convert the passed string to an <code>Int</code>,
 * then passes the resulting <code>Int</code> to <code>beOdd</code>. Thus, you could use
 * <code>beOddAsInt</code> like this:
 * </p>
 *
 * <pre class="stHighlight">
 * "3" should beOddAsInt
 * "4" should not (beOddAsInt)
 * </pre>
 *
 * <h2>Matcher's variance</h2>
 *
 * <p>
 * <code>Matcher</code> is contravariant in its type parameter, <code>T</code>, to make its use more flexible.
 * As an example, consider the hierarchy:
 * </p>
 *
 * <pre class="stHighlight">
 * class Fruit
 * class Orange extends Fruit
 * class ValenciaOrange extends Orange
 * </pre>
 *
 * <p>
 * Given an orange:
 * </p>
 *
 * <pre class="stHighlight">
 * val orange = Orange
 * </pre>
 *
 * <p>
 * The expression "<code>orange should</code>" will, via an implicit conversion in <code>ShouldMatchers</code>,
 * result in an object that has a <code>should</code>
 * method that takes a <code>Matcher[Orange]</code>. If the static type of the matcher being passed to <code>should</code> is
 * <code>Matcher[Valencia]</code> it shouldn't (and won't) compile. The reason it shouldn't compile is that
 * the left value is an <code>Orange</code>, but not necessarily a <code>Valencia</code>, and a
 * <code>Matcher[Valencia]</code> only knows how to match against a <code>Valencia</code>. The reason
 * it won't compile is given that <code>Matcher</code> is contravariant in its type parameter, <code>T</code>, a
 * <code>Matcher[Valencia]</code> is <em>not</em> a subtype of <code>Matcher[Orange]</code>.
 * </p>
 *
 * <p>
 * By contrast, if the static type of the matcher being passed to <code>should</code> is <code>Matcher[Fruit]</code>,
 * it should (and will) compile. The reason it <em>should</em> compile is that given the left value is an <code>Orange</code>,
 * it is also a <code>Fruit</code>, and a <code>Matcher[Fruit]</code> knows how to match against <code>Fruit</code>s.
 * The reason it <em>will</em> compile is that given  that <code>Matcher</code> is contravariant in its type parameter, <code>T</code>, a
 * <code>Matcher[Fruit]</code> is indeed a subtype of <code>Matcher[Orange]</code>.
 * </p>
 *
 * @author Bill Venners
 */
trait Matcher[-T] extends Function1[T, MatchResult] { thisMatcher =>

  /**
   * Check to see if the specified object, <code>left</code>, matches, and report the result in
   * the returned <code>MatchResult</code>. The parameter is named <code>left</code>, because it is
   * usually the value to the left of a <code>should</code> or <code>must</code> invocation. For example,
   * in:
   *
   * <pre class="stHighlight">
   * list should equal (List(1, 2, 3))
   * </pre>
   *
   * The <code>equal (List(1, 2, 3))</code> expression results in a matcher that holds a reference to the
   * right value, <code>List(1, 2, 3)</code>. The <code>should</code> method invokes <code>apply</code>
   * on this matcher, passing in <code>list</code>, which is therefore the "<code>left</code>" value. The
   * matcher will compare the <code>list</code> (the <code>left</code> value) with <code>List(1, 2, 3)</code> (the right
   * value), and report the result in the returned <code>MatchResult</code>.
   *
   * @param left the value against which to match
   * @return the <code>MatchResult</code> that represents the result of the match
   */
  def apply(left: T): MatchResult

  /**
   * Compose this matcher with the passed function, returning a new matcher.
   *
   * <p>
   * This method overrides <code>compose</code> on <code>Function1</code> to
   * return a more specific function type of <code>Matcher</code>. For example, given
   * a <code>beOdd</code> matcher defined like this:
   * </p>
   *
   * <pre class="stHighlight">
   * val beOdd =
   *   new Matcher[Int] {
   *     def apply(left: Int) =
   *       MatchResult(
   *         left % 2 == 1,
   *         left + " was not odd",
   *         left + " was odd"
   *       )
   *   }
   * </pre>
   *
   * <p>
   * You could use <code>beOdd</code> like this:
   * </p>
   *
   * <pre class="stHighlight">
   * 3 should beOdd
   * 4 should not (beOdd)
   * </pre>
   *
   * <p>
   * If for some odd reason, you wanted a <code>Matcher[String]</code> that 
   * checked whether a string, when converted to an <code>Int</code>,
   * was odd, you could make one by composing <code>beOdd</code> with
   * a function that converts a string to an <code>Int</code>, like this:
   * </p>
   *
   * <pre class="stHighlight">
   * val beOddAsInt = beOdd compose { (s: String) => s.toInt }
   * </pre>
   *
   * <p>
   * Now you have a <code>Matcher[String]</code> whose <code>apply</code> method first
   * invokes the converter function to convert the passed string to an <code>Int</code>,
   * then passes the resulting <code>Int</code> to <code>beOdd</code>. Thus, you could use
   * <code>beOddAsInt</code> like this:
   * </p>
   *
   * <pre class="stHighlight">
   * "3" should beOddAsInt
   * "4" should not (beOddAsInt)
   * </pre>
   */
  override def compose[U](g: U => T): Matcher[U] =
    new Matcher[U] {
      def apply(u: U) = thisMatcher.apply(g(u))
    }
}

/**
 * Companion object for trait <code>Matcher</code> that provides a
 * factory method that creates a <code>Matcher[T]</code> from a
 * passed function of type <code>(T => MatchResult)</code>.
 *
 * @author Bill Venners
 */
object Matcher {

  /**
   * Factory method that creates a <code>Matcher[T]</code> from a
   * passed function of type <code>(T => MatchResult)</code>.
   *
   * @author Bill Venners
   */
  def apply[T](fun: T => MatchResult): Matcher[T] =
    new Matcher[T] {
      def apply(left: T) = fun(left)
    }
}

