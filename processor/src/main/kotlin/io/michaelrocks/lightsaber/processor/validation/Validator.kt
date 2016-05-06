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

package io.michaelrocks.lightsaber.processor.validation

import io.michaelrocks.grip.ClassRegistry
import io.michaelrocks.lightsaber.processor.ErrorReporter
import io.michaelrocks.lightsaber.processor.graph.findCycles
import io.michaelrocks.lightsaber.processor.graph.findMissingVertices
import io.michaelrocks.lightsaber.processor.model.InjectionContext

class Validator(
    private val classRegistry: ClassRegistry,
    private val errorReporter: ErrorReporter
) {
  fun validate(context: InjectionContext) {
    performSanityChecks(context)
    validateDependencyGraph(context)
    validateComponentGraph(context)
  }

  private fun performSanityChecks(context: InjectionContext) {
    SanityChecker(classRegistry, errorReporter).performSanityChecks(context)
  }

  private fun validateDependencyGraph(context: InjectionContext) {
    val dependencyGraph = buildDependencyGraph(errorReporter, context.allComponents.flatMap { it.modules })

    val unresolvedDependencies = findMissingVertices(dependencyGraph)
    for (unresolvedDependency in unresolvedDependencies) {
      errorReporter.reportError("Unresolved dependency: $unresolvedDependency")
    }

    val cycles = findCycles(dependencyGraph)
    for (cycle in cycles) {
      errorReporter.reportError("Cycled dependency: ${cycle.joinToString(" -> ")}")
    }
  }

  private fun validateComponentGraph(context: InjectionContext) {
    val componentGraph = buildComponentGraph(context.components)
    val cycles = findCycles(componentGraph)
    for (cycle in cycles) {
      errorReporter.reportError("Cycled component: ${cycle.joinToString(" -> ")}")
    }
  }
}
