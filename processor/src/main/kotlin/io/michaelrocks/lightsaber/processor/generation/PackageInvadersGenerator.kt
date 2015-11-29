/*
 * Copyright 2015 Michael Rozumyanskiy
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

import io.michaelrocks.lightsaber.processor.ProcessorContext
import io.michaelrocks.lightsaber.processor.descriptors.PackageInvaderDescriptor
import io.michaelrocks.lightsaber.processor.logging.getLogger

class PackageInvadersGenerator(
    private val classProducer: ClassProducer,
    private val processorContext: ProcessorContext
) {
  private val logger = getLogger()

  fun generatePackageInvaders() {
    processorContext.getPackageInvaders().forEach { generatePackageInvaders(it) }
  }

  private fun generatePackageInvaders(packageInvader: PackageInvaderDescriptor) {
    logger.debug("Generating package invader {}", packageInvader.type)
    val generator = PackageInvaderClassGenerator(processorContext.typeGraph, packageInvader)
    val classData = generator.generate()
    classProducer.produceClass(packageInvader.type.internalName, classData)
  }
}