/*
 * Copyright 2016 Michael Rozumyanskiy
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

package io.michaelrocks.lightsaber.processor.generation

import io.michaelrocks.lightsaber.processor.ErrorReporter
import io.michaelrocks.lightsaber.processor.ProcessingException
import io.michaelrocks.lightsaber.processor.io.FileSink
import io.michaelrocks.lightsaber.processor.logging.getLogger
import java.io.IOException

class ProcessorSourceProducer(
    private val fileSink: FileSink,
    private val errorReporter: ErrorReporter
) : SourceProducer {
  private val logger = getLogger()

  override fun produceSourceFile(internalName: String, sourceCode: String) {
    logger.debug("Producing source file for {}", internalName)
    val sourceFileName = internalName + ".java"
    try {
      fileSink.createFile(sourceFileName, sourceCode.toByteArray())
    } catch (exception: IOException) {
      val message = "Failed to produce a file:\n$sourceCode"
      errorReporter.reportError(ProcessingException(message, exception, sourceFileName))
    }
  }
}
