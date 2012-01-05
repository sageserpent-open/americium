/*
 * Copyright 2001-2009 Artima, Inc.
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
package org.scalatest.fixture

import org.scalatest._

/**
 * <b>MultipleFixtureFeatureSpec has been deprecated and will be removed in a future version of ScalaTest.
 * Please use FixtureFeatureSpec with ConfigMapFixture instead.</b>
 *
 * <p>
 * I deprecated this trait because I decided using explicit calls to with-fixture methods is preferrable to implicitly
 * calling them, as was recommended and made slightly less concise by this trait. You can of course continue to use
 * the implicit conversion approach if you prefer it. To get rid of this deprecation warning,
 * just change "MultipleFixtureFeatureSpec" to "FixtureFeatureSpec with ConfigMapFixture" in your code. - Bill Venners
 * </p>
 */
@deprecated("Use FixtureFeatureSpec with ConfigMapFixture instead")
trait MultipleFixtureFeatureSpec extends FixtureFeatureSpec with ConfigMapFixture
