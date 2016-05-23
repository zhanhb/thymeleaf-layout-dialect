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
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author zhanhb
 * @param <K>
 * @param <V>
 */
public class ConcurrentWeakIdentityHashMap<K, V> {

    private ConcurrentHashMap<Reference<? extends K>, V> map;
    private ReferenceQueue<K> queue;

    public ConcurrentWeakIdentityHashMap(int size) {
        this.map = new ConcurrentHashMap<>(size);
        this.queue = new ReferenceQueue<>();
    }

    public V get(K key) {
        purgeKeys();
        return map.get(new Key<>(key));
    }

    public V put(K key, V value) {
        purgeKeys();
        return map.put(new Key<>(key, queue), value);
    }

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

    private static class Key<T> extends WeakReference<T> {

        private final int hash;

        Key(T t) {
            super(t);
            hash = System.identityHashCode(t);
        }

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
