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

import collection.mutable.ListBuffer
import events.{InfoProvided, NameInfo}
import java.util.concurrent.atomic.AtomicReference

/*
Spec-like suites buffer up InfoProvided's sent by a test and report them after the
test has either succeeded or failed. This makes the report look nicer.
*/
private[scalatest] abstract class MessageRecordingInformer(nameInfo: NameInfo) extends ConcurrentInformer(nameInfo) {

  private var messages = List[String]()

  protected def shouldRecord = nameInfoForCurrentThread.isDefined

  // Should only be called by the thread that constructed this
  // ConcurrentInformer, because don't want to worry about synchronization here. Just send stuff from
  // other threads whenever. So clients should only call record after first checking shouldRecord
  protected def record(message: String) {
    require(shouldRecord)
    messages ::= message
  }

  // Returns them in order recorded
  def recordedMessages: List[String] = messages.reverse
}