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

import org.scalatest.events.Event
import org.scalatest.events.TestSucceeded

class UseAsFunctionSpec extends FreeSpec {

  "You should be able to use the following as a function after moving to a deprecated implicit conversion:" - {

    "a Reporter" in {
      def takesFun(fun: Event => Unit) {}
      class MyReporter extends Reporter { 
        def apply(e: Event) {}
      }
      takesFun(new MyReporter) // If it compiles, the test passes
    }

    "a Rerunner" in {
      def takesFun(fun: (Reporter, Stopper, Filter, Map[String, Any], Option[Distributor], Tracker, ClassLoader) => Unit) {}
      class MyRerunner extends Rerunner { 
        def apply(reporter: Reporter, stopper: Stopper, filter: Filter,
            configMap: Map[String, Any], distributor: Option[Distributor], tracker: Tracker, loader: ClassLoader) {}
      }
      takesFun(new MyRerunner) // If it compiles, the test passes
    }

    "a Stopper" in {
      def takesFun(fun: () => Boolean) {}
      class MyStopper extends Stopper { 
        override def apply() = true
      }
      takesFun(new MyStopper) // If it compiles, the test passes
    }

    "a Distributor" in {
      def takesFun(fun: (Suite, Tracker) => Unit) {}
      class MyDistributor extends Distributor { 
        def apply(suite: Suite, tracker: Tracker) {}
      }
      takesFun(new MyDistributor) // If it compiles, the test passes
    }
  }
}
