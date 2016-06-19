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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author zhanhb
 * @param <K>
 * @param <V>
 */
class ConcurrentWeakIdentityHashMap<K, V> {

    private final ConcurrentMap<Key<K>, V> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();
    private transient Set<Map.Entry<K, V>> es;

    ConcurrentWeakIdentityHashMap(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }

    public V get(Object key) {
        purgeKeys();
        return map.get(new Key<>(key, null));
    }

    @SuppressWarnings({"NestedAssignment", "element-type-mismatch"})
    private void purgeKeys() {
        Reference<? extends K> reference;
        while ((reference = queue.poll()) != null) {
            map.remove(reference);
        }
    }

    public V putIfAbsent(K key, V value) {
        purgeKeys();
        return map.putIfAbsent(new Key<>(key, queue), value);
    }

    private static class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T t, ReferenceQueue<T> queue) {
            super(t, queue);
            hash = System.identityHashCode(Objects.requireNonNull(t));
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof Key && ((Key<?>) obj).get() == get();
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

}
