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

import java.util.LinkedHashMap;
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
            Object wrappered = getMap(object.getClass()).get(key);
            if (wrappered == null) {
                throw new NoSuchElementException();
            }
            return unwrapper(wrappered);
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
        Map<String, Object> value;
        LinkedHashMap<String, Object> tmp;

        if ((value = map.get(object)) == null
                && (value = map.putIfAbsent(object,
                        tmp = new LinkedHashMap<>(4))) == null) {
            value = tmp;
        }

        return value;
    }

}
