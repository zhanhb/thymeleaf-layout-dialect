/*
 * Copyright 2016 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.net.ultraq.thymeleaf.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author zhanhb
 */
class InMemoryMetaProvider extends MetaProvider {

    private static final Object NULL = new Object();

    private static Object wrapperNull(Object value) {
        return value == null ? NULL : value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrapper(Object value) {
        return value == NULL ? null : (T) value;
    }

    private final ConcurrentMap<Object, Map<String, Object>> map = new ConcurrentWeakIdentityHashMap<>(200);

    @Override
    public <T> T getProperty(Object object, String key) {
        T value = unwrapper(getMap(object).get(key));
        if (value == null) {
            // assert object != null
            List<Object> list = new ArrayList<>(8);
            Class<?> clazz = object.getClass();

            list.add(object);
            while (clazz != null) {
                list.add(clazz);
                Object wrappered = getMap(clazz).get(key);
                if (wrappered != null) {
                    for (Object o : list) {
                        getMap(o).put(key, wrappered);
                    }
                    return unwrapper(wrappered);
                }
                clazz = clazz.getSuperclass();
            }
            for (Class<?> aInterface : object.getClass().getInterfaces()) {
                Object wrappered = getMap(aInterface).get(key);
                if (wrappered != null) {
                    getMap(object).put(key, wrappered);
                    getMap(object.getClass()).put(key, wrappered);
                    return unwrapper(wrappered);
                }
            }
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public void setProperty(Object object, String key, Object value) {
        getMap(object).put(key, wrapperNull(value));
    }

    @Override
    public void removeProperty(Object object, String key) {
        getMap(object).remove(key);
    }

    @SuppressWarnings("NestedAssignment")
    private Map<String, Object> getMap(Object object) {
        Map<String, Object> oldValue, newValue;
        return ((oldValue = map.get(object)) == null
                && (oldValue = map.putIfAbsent(object,
                        newValue = new LinkedHashMap<>(4))) == null)
                        ? newValue : oldValue;
    }

}
