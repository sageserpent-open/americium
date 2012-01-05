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

class InformerSpec extends FlatSpec {

  "An Informer" should "give back another Informer from its compose method" in {
    var lastS = "2"
    val myInfo =
      new Informer {
        def apply(s: String) { lastS = s }
      }
    
    val composed = myInfo compose { (i: Int) => (i + 1).toString }
    composed(1)
    assert(lastS === "2")
  }
}
