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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author zhanhb
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
class ConcurrentWeakIdentityHashMap<K, V> {

    private final ConcurrentMap<Object, V> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    ConcurrentWeakIdentityHashMap(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }

    public V get(Object key) {
        purgeKeys();
        return map.get(new Key<>(key, null));
    }

    @SuppressWarnings("NestedAssignment")
    private void purgeKeys() {
        Reference<? extends K> reference;
        ReferenceQueue<K> q = this.queue;
        ConcurrentMap<?, V> m = this.map;
        while ((reference = q.poll()) != null) {
            m.remove(reference);
        }
    }

    public V putIfAbsent(K key, V value) {
        purgeKeys();
        return map.putIfAbsent(new Key<>(key, queue), value);
    }

    public int size() {
        purgeKeys();
        return map.size();
    }

    private static class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T t, ReferenceQueue<T> queue) {
            super(Objects.requireNonNull(t), queue);
            hash = System.identityHashCode(t);
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
