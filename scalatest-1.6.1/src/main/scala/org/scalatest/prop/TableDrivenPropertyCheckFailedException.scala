/*
 * Copyright 2001-2011 Artima, Inc.
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
package org.scalatest
package prop

/**
 * Exception that indicates a table-driven property check failed.
 *
 * <p>
 * For an introduction to using tables, see the documentation for trait
 * <a href="TableDrivenPropertyChecks.html">TableDrivenPropertyChecks</a>.
 * </p>
 *
 * @param messageFun a function that returns a detail message, not optional) for this <code>PropertyCheckFailedException</code>.
 * @param cause an optional cause, the <code>Throwable</code> that caused this <code>PropertyCheckFailedException</code> to be thrown.
 * @param failedCodeStackDepthFun a function that returns the depth in the stack trace of this exception at which the line of test code that failed resides.
 * @param undecoratedMessage just a short message that has no redundancy with args, labels, etc. The regular "message" has everything in it
 * @param args the argument values
 * @param namesOfArgs a list of string names for the arguments
 * @param row the index of the table row that failed the property check, causing this exception to be thrown
 *
 * @throws NullPointerException if any parameter is <code>null</code> or <code>Some(null)</code>.
 *
 * @author Bill Venners
 */
class TableDrivenPropertyCheckFailedException(
  messageFun: StackDepthException => String,
  cause: Option[Throwable],
  failedCodeStackDepthFun: StackDepthException => Int,
  undecoratedMessage: String,
  args: List[Any],
  namesOfArgs: List[String],
  val row: Int
) extends PropertyCheckFailedException(messageFun, cause, failedCodeStackDepthFun, undecoratedMessage, args, Some(namesOfArgs))
