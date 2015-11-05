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

package io.michaelrocks.lightsaber.processor.generation;

import io.michaelrocks.lightsaber.processor.ProcessorContext;
import io.michaelrocks.lightsaber.processor.commons.GeneratorAdapter;
import io.michaelrocks.lightsaber.processor.commons.StandaloneClassWriter;
import io.michaelrocks.lightsaber.processor.commons.Types;
import io.michaelrocks.lightsaber.processor.descriptors.FieldDescriptor;
import io.michaelrocks.lightsaber.processor.descriptors.InjectorDescriptor;
import io.michaelrocks.lightsaber.processor.descriptors.MethodDescriptor;
import io.michaelrocks.lightsaber.processor.descriptors.ModuleDescriptor;
import io.michaelrocks.lightsaber.processor.watermark.WatermarkClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class LightsaberRegistryClassGenerator {
    private static final Type LIGHTSABER_REGISTRY_TYPE =
            Type.getObjectType("io/michaelrocks/lightsaber/LightsaberRegistry");
    private static final Type LIST_TYPE = Type.getType(List.class);
    private static final Type ARRAY_LIST_TYPE = Type.getType(ArrayList.class);
    private static final Type MAP_TYPE = Type.getType(Map.class);
    private static final Type HASH_MAP_TYPE = Type.getType(HashMap.class);

    private static final FieldDescriptor PACKAGE_INJECTOR_CONFIGURATORS_FIELD =
            new FieldDescriptor("packageInjectorConfigurators", LIST_TYPE);
    private static final FieldDescriptor INJECTOR_CONFIGURATORS_FIELD =
            new FieldDescriptor("injectorConfigurators", MAP_TYPE);
    private static final FieldDescriptor MEMBERS_INJECTORS_FIELD =
            new FieldDescriptor("membersInjectors", MAP_TYPE);

    private static final MethodDescriptor ARRAY_LIST_CONSTRUCTOR =
            MethodDescriptor.forConstructor(Type.INT_TYPE);
    private static final MethodDescriptor ADD_METHOD =
            MethodDescriptor.forMethod("add", Type.BOOLEAN_TYPE, Types.OBJECT_TYPE);
    private static final MethodDescriptor HASH_MAP_CONSTRUCTOR =
            MethodDescriptor.forConstructor(Type.INT_TYPE);
    private static final MethodDescriptor PUT_METHOD =
            MethodDescriptor.forMethod("put", Types.OBJECT_TYPE, Types.OBJECT_TYPE, Types.OBJECT_TYPE);

    private static final MethodDescriptor GET_PACKAGE_INJECTOR_CONFIGURATORS_METHOD =
            MethodDescriptor.forMethod("getPackageInjectorConfigurators", LIST_TYPE);
    private static final MethodDescriptor GET_INJECTOR_CONFIGURATORS_METHOD =
            MethodDescriptor.forMethod("getInjectorConfigurators", MAP_TYPE);
    private static final MethodDescriptor GET_MEMBERS_INJECTORS_METHOD =
            MethodDescriptor.forMethod("getMembersInjectors", MAP_TYPE);

    private final ClassProducer classProducer;
    private final ProcessorContext processorContext;

    public LightsaberRegistryClassGenerator(final ClassProducer classProducer,
            final ProcessorContext processorContext) {
        this.classProducer = classProducer;
        this.processorContext = processorContext;
    }

    public void generateLightsaberRegistry() {
        final ClassWriter classWriter =
                new StandaloneClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, processorContext);
        final ClassVisitor classVisitor = new WatermarkClassVisitor(classWriter, true);
        classVisitor.visit(
                V1_6,
                ACC_PUBLIC | ACC_SUPER,
                LIGHTSABER_REGISTRY_TYPE.getInternalName(),
                null,
                Type.getInternalName(Object.class),
                new String[] { Type.getInternalName(Provider.class) });

        generateFields(classVisitor);
        generateStaticInitializer(classVisitor);
        generateMethods(classVisitor);

        classVisitor.visitEnd();
        final byte[] classBytes = classWriter.toByteArray();
        classProducer.produceClass(LIGHTSABER_REGISTRY_TYPE.getInternalName(), classBytes);
    }

    private void generateFields(final ClassVisitor classVisitor) {
        generateInjectorConfiguratorsField(classVisitor);
        generateMembersInjectorsField(classVisitor);
        generatePackageModulesField(classVisitor);
    }

    private void generateInjectorConfiguratorsField(final ClassVisitor classVisitor) {
        final FieldVisitor fieldVisitor = classVisitor.visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                INJECTOR_CONFIGURATORS_FIELD.getName(),
                INJECTOR_CONFIGURATORS_FIELD.getDescriptor(),
                null,
                null);
        fieldVisitor.visitEnd();
    }

    private void generateMembersInjectorsField(final ClassVisitor classVisitor) {
        final FieldVisitor fieldVisitor = classVisitor.visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                MEMBERS_INJECTORS_FIELD.getName(),
                MEMBERS_INJECTORS_FIELD.getDescriptor(),
                null,
                null);
        fieldVisitor.visitEnd();
    }

    private void generatePackageModulesField(final ClassVisitor classVisitor) {
        final FieldVisitor fieldVisitor = classVisitor.visitField(
                ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                PACKAGE_INJECTOR_CONFIGURATORS_FIELD.getName(),
                PACKAGE_INJECTOR_CONFIGURATORS_FIELD.getDescriptor(),
                null,
                null);
        fieldVisitor.visitEnd();
    }

    private void generateStaticInitializer(final ClassVisitor classVisitor) {
        final GeneratorAdapter generator =
                new GeneratorAdapter(classVisitor, ACC_STATIC, MethodDescriptor.forStaticInitializer());
        generator.visitCode();

        populatePackageInjectorConfiguratorsMethod(generator);
        populateInjectorConfigurators(generator);
        populateMembersInjectors(generator);

        generator.returnValue();
        generator.endMethod();
    }

    private void populatePackageInjectorConfiguratorsMethod(final GeneratorAdapter generator) {
        final Collection<ModuleDescriptor> packageModules = processorContext.getPackageModules();
        generator.newInstance(ARRAY_LIST_TYPE);
        generator.dup();
        generator.push(packageModules.size());
        generator.invokeConstructor(ARRAY_LIST_TYPE, ARRAY_LIST_CONSTRUCTOR);

        for (final ModuleDescriptor packageModule : packageModules) {
            generator.dup();
            generator.newInstance(packageModule.getConfiguratorType());
            generator.dup();
            generator.invokeConstructor(packageModule.getConfiguratorType(), MethodDescriptor.forDefaultConstructor());
            generator.invokeInterface(LIST_TYPE, ADD_METHOD);
            generator.pop();
        }

        generator.putStatic(LIGHTSABER_REGISTRY_TYPE, PACKAGE_INJECTOR_CONFIGURATORS_FIELD);
    }

    private void populateInjectorConfigurators(final GeneratorAdapter generator) {
        final Collection<ModuleDescriptor> modules = processorContext.getModules();
        generator.newInstance(HASH_MAP_TYPE);
        generator.dup();
        generator.push(modules.size());
        generator.invokeConstructor(HASH_MAP_TYPE, HASH_MAP_CONSTRUCTOR);

        for (final ModuleDescriptor module : modules) {
            generator.dup();
            generator.push(module.getModuleType());
            generator.newInstance(module.getConfiguratorType());
            generator.dup();
            generator.invokeConstructor(module.getConfiguratorType(), MethodDescriptor.forDefaultConstructor());
            generator.invokeInterface(MAP_TYPE, PUT_METHOD);
            generator.pop();
        }

        generator.putStatic(LIGHTSABER_REGISTRY_TYPE, INJECTOR_CONFIGURATORS_FIELD);
    }

    private void populateMembersInjectors(final GeneratorAdapter generator) {
        final Collection<InjectorDescriptor> injectors = processorContext.getInjectors();
        generator.newInstance(HASH_MAP_TYPE);
        generator.dup();
        generator.push(injectors.size());
        generator.invokeConstructor(HASH_MAP_TYPE, HASH_MAP_CONSTRUCTOR);

        for (final InjectorDescriptor injector : injectors) {
            generator.dup();
            generator.push(injector.getInjectableTarget().getTargetType());
            generator.newInstance(injector.getInjectorType());
            generator.dup();
            generator.invokeConstructor(injector.getInjectorType(), MethodDescriptor.forDefaultConstructor());
            generator.invokeInterface(MAP_TYPE, PUT_METHOD);
            generator.pop();
        }

        generator.putStatic(LIGHTSABER_REGISTRY_TYPE, MEMBERS_INJECTORS_FIELD);
    }

    private void generateMethods(final ClassVisitor classVisitor) {
        generateGetInjectorGonfiguratorsMethod(classVisitor);
        generateGetMembersInjectorsMethod(classVisitor);
        generateGetPackageModulesMethod(classVisitor);
    }

    private void generateGetInjectorGonfiguratorsMethod(final ClassVisitor classVisitor) {
        final GeneratorAdapter generator =
                new GeneratorAdapter(classVisitor, ACC_PUBLIC | ACC_STATIC, GET_INJECTOR_CONFIGURATORS_METHOD);
        generator.visitCode();
        generator.getStatic(LIGHTSABER_REGISTRY_TYPE, INJECTOR_CONFIGURATORS_FIELD);
        generator.returnValue();
        generator.endMethod();
    }

    private void generateGetMembersInjectorsMethod(final ClassVisitor classVisitor) {
        final GeneratorAdapter generator =
                new GeneratorAdapter(classVisitor, ACC_PUBLIC | ACC_STATIC, GET_MEMBERS_INJECTORS_METHOD);
        generator.visitCode();
        generator.getStatic(LIGHTSABER_REGISTRY_TYPE, MEMBERS_INJECTORS_FIELD);
        generator.returnValue();
        generator.endMethod();
    }

    private void generateGetPackageModulesMethod(final ClassVisitor classVisitor) {
        final GeneratorAdapter generator =
                new GeneratorAdapter(classVisitor, ACC_PUBLIC | ACC_STATIC, GET_PACKAGE_INJECTOR_CONFIGURATORS_METHOD);
        generator.visitCode();
        generator.getStatic(LIGHTSABER_REGISTRY_TYPE, PACKAGE_INJECTOR_CONFIGURATORS_FIELD);
        generator.returnValue();
        generator.endMethod();
    }
}
