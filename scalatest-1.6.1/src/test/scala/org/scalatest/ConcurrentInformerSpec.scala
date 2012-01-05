package org.scalatest

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

import events.NameInfo
import fixture.FixtureSpec
import java.util.concurrent.atomic.AtomicBoolean

class ConcurrentInformerSpec extends FixtureSpec {

  val nameInfo = NameInfo("suite name", Some("suite.class.Name"), Some("test name"))

  type FixtureParam = ConcurrentInformer

  // The ConcurrentInformer must be passed in rather than constructed in the constructor
  // and shared that way, to make sure it is created by the same thread that runs the tests
  def withFixture(test: OneArgTest) {
    val informer =
      new ConcurrentInformer(nameInfo) {
        def apply(message: String) = ()
      }
    test(informer)
  }

  describe("A ConcurrentInformer") {
    it("should return the passed NameInfo in a Some when the constructing thread calls nameInfoForCurrentThread") { informer =>
      assert(informer.nameInfoForCurrentThread.isDefined)
      assert(informer.nameInfoForCurrentThread.get === nameInfo)
    }
    it("should return None when a thread other than the constructing thread calls nameInfoForCurrentThread") { informer =>
      val nameInfoWasNone = new AtomicBoolean
      class MyThread extends Thread {
        override def run() {
          nameInfoWasNone.set(!informer.nameInfoForCurrentThread.isDefined)
        }
      }
      val thread = new MyThread
      thread.start()
      thread.join()
      assert(nameInfoWasNone.get)
    }
  }
}
