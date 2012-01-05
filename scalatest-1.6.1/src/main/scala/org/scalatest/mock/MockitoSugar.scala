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
package org.scalatest.mock

import org.scalatest._
import org.mockito.Mockito.{mock => mockitoMock}
import reflect.Manifest

/**
 * Trait that provides some basic syntax sugar for <a href="http://mockito.org/" target="_blank">Mockito</a>.
 *
 * <p>
 * Using the Mockito API directly, you create a mock with:
 * </p>
 *
 * <pre class="stHighlight">
 * val mockCollaborator = mock(classOf[Collaborator])
 * </pre>
 *
 * <p>
 * Using this trait, you can shorten that to:
 * </p>
 *
 * <pre class="stHighlight">
 * val mockCollaborator = mock[Collaborator]
 * </pre>
 *
 * @author Bill Venners
 */
trait MockitoSugar {

  /**
   * Invokes the <code>mock</code> method on the <code>Mockito</code> companion object (<em>i.e.</em>, the
   * static <code>mock</code> method in Java class <code>org.mockito.Mockitok</code>).
   *
   * <p>
   * Using the Mockito API directly, you create a mock with:
   * </p>
   *
   * <pre class="stHighlight">
   * val mockCollaborator = mock(classOf[Collaborator])
   * </pre>
   *
   * <p>
   * Using this method, you can shorten that to:
   * </p>
   *
   * <pre class="stHighlight">
   * val mockCollaborator = mock[Collaborator]
   * </pre>
   */
  def mock[T <: AnyRef](implicit manifest: Manifest[T]): T = {
    mockitoMock(manifest.erasure.asInstanceOf[Class[T]])
  }
}
