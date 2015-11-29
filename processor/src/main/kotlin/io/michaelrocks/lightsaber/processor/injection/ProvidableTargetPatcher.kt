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

package io.michaelrocks.lightsaber.processor.injection

import io.michaelrocks.lightsaber.processor.ProcessorContext
import io.michaelrocks.lightsaber.processor.descriptors.InjectionTargetDescriptor
import io.michaelrocks.lightsaber.processor.descriptors.MethodDescriptor
import io.michaelrocks.lightsaber.processor.descriptors.isInjectableConstructor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

import org.objectweb.asm.Opcodes.*

class ProvidableTargetPatcher(
    processorContext: ProcessorContext,
    classVisitor: ClassVisitor,
    private val providableTarget: InjectionTargetDescriptor
) : BaseInjectionClassVisitor(processorContext, classVisitor) {

  override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?,
      interfaces: Array<String>?) {
    val newAccess = (access and (ACC_PRIVATE or ACC_PROTECTED).inv()) or ACC_PUBLIC
    super.visit(version, access, name, signature, superName, interfaces)
    if (newAccess != access) {
      isDirty = true
    }
  }

  override fun visitMethod(access: Int, name: String, desc: String, signature: String?,
      exceptions: Array<String>?): MethodVisitor? {
    val methodDescriptor = MethodDescriptor(name, desc)
    if (providableTarget.isInjectableConstructor(methodDescriptor)) {
      val newAccess = access and ACC_PRIVATE.inv()
      if (newAccess != access) {
        isDirty = true
      }
      return super.visitMethod(newAccess, name, desc, signature, exceptions)
    } else {
      return super.visitMethod(access, name, desc, signature, exceptions)
    }
  }
}