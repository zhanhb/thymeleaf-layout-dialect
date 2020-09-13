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
package nz.net.ultraq.thymeleaf.context.extensions;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author zhanhb
 */
public class ConcurrentWeakIdentityHashMapTest {

    private ConcurrentWeakIdentityHashMap<Object, Object> instance;

    @Before
    public void setUp() {
        instance = new ConcurrentWeakIdentityHashMap<>(16);
    }

    @Test
    @SuppressWarnings("SleepWhileInLoop")
    public void testEnsureRemoved() throws InterruptedException {
        instance.putIfAbsent(new Object(), new Object());
        assertEquals(1, instance.size());
        System.gc();
        for (int i = 0; i < 500; ++i) {
            Thread.sleep(20);
            if (instance.size() == 0) {
                break;
            }
        }
        assertEquals(0, instance.size());
    }

    @Test
    public void testMutableKey() {
        Map<Object, Object> key = new HashMap<>(1);
        instance.putIfAbsent(key, key);
        // change key hashCode
        key.put("test1", "test");
        assertSame(key, instance.get(key));
        assertEquals(1, instance.size());
        key.remove("test1");
        assertSame(key, instance.get(key));
    }

    @Test
    public void testGetNull() {
        // should be ok
        instance.get(null);
    }

}
