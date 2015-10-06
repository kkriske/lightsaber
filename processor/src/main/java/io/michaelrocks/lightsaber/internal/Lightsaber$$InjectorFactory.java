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

package io.michaelrocks.lightsaber.internal;

import io.michaelrocks.lightsaber.ConfigurationException;
import io.michaelrocks.lightsaber.Injector;
import io.michaelrocks.lightsaber.Key;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Lightsaber$$InjectorFactory {
    private static final Key<?> INJECTOR_KEY = Key.of(Injector.class);

    private static final Map<Class, TypeAgent> typeAgents = new HashMap<>();

    static {
        populateTypeAgents();
    }

    private static void populateTypeAgents() {
        // This method will be generated.
    }

    private static void registerTypeAgent(final TypeAgent<?> typeAgent) {
        typeAgents.put(typeAgent.getType(), typeAgent);
    }

    private static Object[] getPackageModules() {
        // This method will be generated.
        return new Object[] {};
    }

    public static Injector createInjector(final Object... modules) {
        final LightsaberInjector injector = createChildInjectorInternal(null, modules);
        configureInjector(injector, getPackageModules());
        return injector;
    }

    public static Injector createChildInjector(final Injector parentInjector, final Object... modules) {
        if (parentInjector == null) {
            throw new NullPointerException("Parent injector cannot be null");
        }
        return createChildInjectorInternal(parentInjector, modules);
    }

    private static LightsaberInjector createChildInjectorInternal(final Injector parentInjector,
            final Object... modules) {
        final LightsaberInjector injector = new LightsaberInjector(parentInjector);
        configureInjector(injector, modules);
        checkProvidersNotOverlap(injector, parentInjector);
        return injector;
    }

    private static void configureInjector(final LightsaberInjector injector, final Object[] modules) {
        if (modules != null) {
            for (final Object module : modules) {
                if (module == null) {
                    throw new NullPointerException("Trying to create injector with a null module");
                }

                if (!(module instanceof ConfigurableModule)) {
                    throw new ConfigurationException("Module " + module + " hasn't been processed");
                }

                ((ConfigurableModule) module).configureInjector(injector);
            }
        }
    }

    private static void checkProvidersNotOverlap(final LightsaberInjector injector, final Injector parentInjector) {
        if (parentInjector == null) {
            return;
        }

        final Set<Key<?>> overlappingKeys = new HashSet<>(injector.getProviders().keySet());
        overlappingKeys.retainAll(parentInjector.getAllProviders().keySet());
        overlappingKeys.remove(INJECTOR_KEY);
        if (!overlappingKeys.isEmpty()) {
            throw new ConfigurationException(composeOverlappingKeysMessage(overlappingKeys));
        }
    }

    private static String composeOverlappingKeysMessage(final Set<Key<?>> overlappingKeys) {
        final StringBuilder builder = new StringBuilder("Injector has overlapping keys with its parent:");
        for (final Key<?> key : overlappingKeys) {
            builder.append("\n  ").append(key);
        }
        return builder.toString();
    }

    public static void injectMembers(final Injector injector, final Object object) {
        injectFieldsIntoObject(injector, object, object.getClass());
        injectMethodsIntoObject(injector, object, object.getClass());
    }

    private static void injectFieldsIntoObject(final Injector injector, final Object object, final Class type) {
        if (type == Object.class) {
            return;
        }

        injectFieldsIntoObject(injector, object, type.getSuperclass());
        final TypeAgent typeAgent = typeAgents.get(type);
        if (typeAgent != null) {
            // noinspection unchecked
            typeAgent.injectFields(injector, object);
        }
    }

    private static void injectMethodsIntoObject(final Injector injector, final Object object, final Class type) {
        if (type == Object.class) {
            return;
        }

        injectMethodsIntoObject(injector, object, type.getSuperclass());
        final TypeAgent typeAgent = typeAgents.get(type);
        if (typeAgent != null) {
            // noinspection unchecked
            typeAgent.injectMethods(injector, object);
        }
    }
}
