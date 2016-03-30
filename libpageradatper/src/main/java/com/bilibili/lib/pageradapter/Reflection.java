/*
 * Copyright (C) 2016 Bilibili, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bilibili.lib.pageradapter;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yrom.
 */
final class Reflection {
    private static final Map<String, Field> sFieldCache = new HashMap<>();

    private Class<?> mClass;

    private Reflection(Class<?> clazz) {
        mClass = clazz;
    }

    <T> T fieldValue(Object instance, String name) throws NoSuchFieldException {
        Field field = findField(mClass, name);
        if (!field.isAccessible()) {
            setAccessible(field);
        }
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    static Reflection on(Class<?> cls) {
        return new Reflection(cls);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        String fullFieldName = clazz.getName() + '#' + name;

        if (sFieldCache.containsKey(fullFieldName)) {
            Field field = sFieldCache.get(fullFieldName);
            if (field == null)
                throw new NoSuchFieldException(name);
            return field;
        }

        try {
            Field field = findFieldRecursive(clazz, name);
            setAccessible(field);
            sFieldCache.put(fullFieldName, field);
            return field;
        } catch (NoSuchFieldException e) {
            sFieldCache.put(fullFieldName, null);
            throw new NoSuchFieldException(name);
        }
    }

    private static Field findFieldRecursive(Class<?> clazz, String name) throws NoSuchFieldException {
        NoSuchFieldException exception = null;
        do {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
                if (exception == null) exception = e;
            }
        } while (clazz != null && !clazz.equals(Object.class));

        throw exception;

    }

    private static void setAccessible(AccessibleObject object) {
        object.setAccessible(true);
    }
}
