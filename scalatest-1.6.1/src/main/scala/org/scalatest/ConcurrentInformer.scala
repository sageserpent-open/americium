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

import events.NameInfo
import java.util.concurrent.atomic.AtomicReference

/*
 This is used by Suite and test informers created as tests run, which therefore have
 populated defined NameInfos. These informers are returned by info in FunSuite and Spec,
 or passed to test methods that take an Informer in Suite, for example. If called by the
 thread that constructed them, which is the thread that was executing the suite and the tests
 inside the suite, then that NameInfo should be propagated. However, if a test starts other
 threads for a multi-threaded test, and those threads apply the Informer, then the NameInfo
 should *not* be propagated, because otherwise it could become very confusing to figure out
 what came from where in the report. Threads started by the test could outlast the thread
 that was running the test, for example. There will be a thread-name, so they can use that
 to figure out who sent what. And threads that call these informers will share a Tracker with
 the thread that was running the tests, so they should be ordered close together after
 sorting by Ordinal. But that's it. NameInfo only goes out when the thread running a test
 or suite applies the Informer.

 This in turn means that a reporter may get hit by multiple threads sending InfoProvided
 messages. If run with the Runner, that will be OK, because DispatchReporter will be in front
 serializing events with its actor. If run() is invoked directly on a suite instance, such as
 from the Scala interpretter, then it may not work. I think I may just say that when running
 from the interpreter, say with run(), you may get interleaved output. This would only happen
 when doing a multi-threaded test that starts threads that calls informer methods, likely a
 rare case. Also, in that case I think it is reasonable to say you may get interleaved output
 in the interpreter, so if you don't like that, use the Runner.
*/
private[scalatest] abstract class ConcurrentInformer(nameInfo: NameInfo) extends Informer {

  private final val atomic = new AtomicReference[(Thread, Option[NameInfo])](Thread.currentThread, Some(nameInfo))

  def nameInfoForCurrentThread: Option[NameInfo] = {
    val (constructingThread, nameInfo) = atomic.get
    if (Thread.currentThread == constructingThread) nameInfo else None
  }

  def isConstructingThread: Boolean = {
    val (constructingThread, _) = atomic.get
    Thread.currentThread == constructingThread
  }
}

private[scalatest] abstract class ThreadAwareInformer extends Informer {

  private final val atomic = new AtomicReference[Thread](Thread.currentThread)

  def isConstructingThread: Boolean = {
    val constructingThread = atomic.get
    Thread.currentThread == constructingThread
  }
}

// Getting rid of the NameInfo passed to ConcurrentInformer. After go through entire
// refactor can rename ConcurrentInformer2 to ConcurrentInformer, deleting the old one
private[scalatest] class ConcurrentInformer2(fire: (String, Boolean) => Unit) extends ThreadAwareInformer {

  def apply(message: String) {
    if (message == null)
      throw new NullPointerException
    fire(message, isConstructingThread) // Fire the info provided event using the passed function
  }
}

private[scalatest] object ConcurrentInformer2 {
  def apply(fire: (String, Boolean) => Unit) = new ConcurrentInformer2(fire)
}

//
// Three params of function are the string message, a boolean indicating this was from the current thread, and
// the last one is an optional boolean that indicates the message is about a pending test, in which case it would
// be printed out in yellow.
//
// This kind of informer is only used during the execution of tests, to delay the printing out of info's fired
// during tests until after the test succeeded, failed, or pending gets sent out.
//
private[scalatest] class MessageRecordingInformer2(fire: (String, Boolean, Boolean) => Unit) extends ThreadAwareInformer {

  private var messages = List[String]()

  // Should only be called by the thread that constructed this
  // ConcurrentInformer, because don't want to worry about synchronization here. Just send stuff from
  // other threads whenever they come in. So only call record after first checking isConstructingThread
  private def record(message: String) {
    require(isConstructingThread)
    messages ::= message
  }

  // Returns them in order recorded
  private def recordedMessages: List[String] = messages.reverse

  def apply(message: String) {
    if (message == null)
      throw new NullPointerException
    if (isConstructingThread)
      record(message)
    else 
      fire(message, false, false) // Fire the info provided event using the passed function
  }

  // send out any recorded messages
  def fireRecordedMessages(testWasPending: Boolean) {
    for (message <- recordedMessages)
      fire(message, true, testWasPending) // Fire the info provided event using the passed function
  }
}

private[scalatest] object MessageRecordingInformer2 {
  def apply(fire: (String, Boolean, Boolean) => Unit) = new MessageRecordingInformer2(fire)
}
