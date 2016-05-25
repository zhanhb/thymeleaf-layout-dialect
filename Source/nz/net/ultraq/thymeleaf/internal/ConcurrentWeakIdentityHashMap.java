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
import java.util.AbstractMap;
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
public class ConcurrentWeakIdentityHashMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V> {

    private final ConcurrentHashMap<Reference<? extends K>, V> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    public ConcurrentWeakIdentityHashMap(int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public ConcurrentWeakIdentityHashMap() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public V get(Object key) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.get(new Key<>(key, null));
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.put(new Key<>(key, queue), value);
    }

    @Override
    public int size() {
        purgeKeys();
        return map.size();
    }

    @SuppressWarnings("NestedAssignment")
    private void purgeKeys() {
        Reference<? extends K> reference;
        while ((reference = queue.poll()) != null) {
            map.remove(reference);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.putIfAbsent(new Key<>(key, queue), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.remove(new Key<>(key, null), value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.replace(new Key<>(key, null), oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.replace(new Key<>(key, null), value);
    }

    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key);
        purgeKeys();
        return map.containsKey(new Key<>(key, null));
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void clear() {
        while (queue.poll() != null);
        map.clear();
    }

    private static class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T t, ReferenceQueue<T> queue) {
            super(t, queue);
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
