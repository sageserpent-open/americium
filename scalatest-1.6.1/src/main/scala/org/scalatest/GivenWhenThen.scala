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
package org.scalatest

/**
 * Trait that contains methods named <code>given</code>, <code>when</code>, <code>then</code>, and <code>and</code>,
 * which take a string message and implicit <code>Informer</code>, and forward the message to the informer.
 *
 * @author Bill Venners
 */
trait GivenWhenThen {

  /**
   * Forwards a message to an implicit <code>Informer</code>, preceded by "Given."
   *
   * @param message the message to forward to the passed informer
   * @param info the <code>Informer</code> to which to forward the message
   */
  def given(message: String)(implicit info: Informer) {
    info(Resources("givenMessage", message))
  }

  /**
   * Forwards a message to an implicit <code>Informer</code>, preceded by "When ".
   *
   * @param message the message to forward to the passed informer
   * @param info the <code>Informer</code> to which to forward the message
   */
  def when(message: String)(implicit info: Informer) {
    info(Resources("whenMessage", message))
  }

  /**
   * Forwards a message to an implicit <code>Informer</code>, preceded by "Then ".
   *
   * @param message the message to forward to the passed informer
   * @param info the <code>Informer</code> to which to forward the message
   */
  def then(message: String)(implicit info: Informer) {
    info(Resources("thenMessage", message))
  }

  /**
   * Forwards a message to an implicit <code>Informer</code>, preceded by "And ".
   *
   * @param message the message to forward to the passed informer
   * @param info the <code>Informer</code> to which to forward the message
   */
  def and(message: String)(implicit info: Informer) {
    info(Resources("andMessage", message))
  }
}

/**
 * Companion object that facilitates the importing of <code>GivenWhenThen</code> members as
 * an alternative to mixing it in.
 *
 * @author Bill Venners
 */
object GivenWhenThen extends GivenWhenThen
