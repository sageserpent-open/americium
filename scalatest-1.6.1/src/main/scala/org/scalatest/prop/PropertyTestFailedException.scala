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
package prop

/**
 * <strong>This exception class has been deprecated and will be removed in a future version of ScalaTest. Please
 * change uses of this class to use <code>GeneratorDrivenPropertyCheckFailedException</code> instead.</strong>
 *
 * @author Bill Venners
 */
@deprecated("Please use GeneratorDrivenPropertyCheckFailedException instead.")
class PropertyTestFailedException(
  message: String,
  cause: Option[Throwable],
  failedCodeStackDepth: Int,
  undecoratedMessage: String,
  args: List[Any],
  labels: List[String]
) extends GeneratorDrivenPropertyCheckFailedException(sde => message, cause, sde => failedCodeStackDepth, undecoratedMessage, args, None, labels)
