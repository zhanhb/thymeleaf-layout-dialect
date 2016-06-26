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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 * @author zhanhb
 */
public class MetaProviderTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final MetaProvider provider1 = new GroovyMetaProvider(), provider2 = new InMemoryMetaProvider();

    @Test
    public void test() {
        test((provider, key) -> {
            test(provider, key, "123");
        });
        test((provider, key) -> {
            test(provider, key, null);
        });
    }

    private void test(MetaProvider provider, String key, String value) {
        provider.setProperty(T.class, key, value);
        assertEquals(value, provider.getProperty(new T(), key));
    }

    @Test
    public void testClass() {
        test((provider, key) -> {
            testClass(provider, key, "456");
        });
        test((provider, key) -> {
            testClass(provider, key, null);
        });
    }

    private void testClass(MetaProvider provider, String key, String value) {
        provider.setProperty(Class.class, key, value);
        assertEquals(value, provider.getProperty(T.class, key));
    }

    @Test
    public void testInherit() {
        final String value = "0";
        test((provider, key) -> {
            provider.setProperty(T.class, key, value);
            provider.setProperty(Class.class, key, value + 1);
            assertEquals(provider.toString(), value + 1, provider.getProperty(S.class, key));
        });
        test((provider, key) -> {
            provider.setProperty(Class.class, key, value);
            assertEquals(provider.toString(), value, provider.getProperty(S.class, key));
        });
    }

    @Test
    public void testInheritException() {
        test((provider, key) -> {
            provider.setProperty(T.class, key, "0");
            try {
                provider.getProperty(S.class, key);
                fail();
            } catch (RuntimeException ex) {
                // ok
            }
        });
    }

    @Test
    public void testException() {
        test((provider, key) -> {
            try {
                provider.getProperty(new Object(), key);
                fail();
            } catch (RuntimeException ex) {
                // ok
            }
        });
    }

    @Test
    public void testNullObject() {
        test((provider, key) -> {
            try {
                provider.getProperty(null, key);
                fail();
            } catch (NullPointerException ex) {
                // ok
            }
        });
    }

    @Test
    public void testNullKey() {
        test((provider, key) -> {
            try {
                provider.getProperty(key, null);
                fail();
            } catch (RuntimeException ex) {
                // ok
            }
        });
    }

    @Test
    public void testEnsureCached() {
        final String value = "ensure cached";
        test((provider, key) -> {
            provider.setProperty(Object.class, key, value);
            provider.getProperty(new S(), key);
            provider.setProperty(Object.class, key, null);
            assertEquals(provider.toString(), value, provider.getProperty(new S(), key));
            assertEquals(provider.toString(), value, provider.getProperty(new T(), key));
            assertNull(provider.getProperty(new Object(), key));
        });
        test((provider, key) -> {
            provider.setProperty(Object.class, key, value);
            provider.setProperty(Object.class, key, null);
            assertNull(provider.getProperty(new S(), key));
            assertNull(provider.getProperty(new T(), key));
            assertNull(provider.getProperty(new Object(), key));
        });
    }

    @Test
    public void testInterface() {
        test((provider, key) -> {
            provider.setProperty(Serializable.class, key, "1");
            provider.setProperty(T.class, key, "2");
            assertEquals(provider.toString(), "2", provider.getProperty(new S(), key));
        });
        test((provider, key) -> {
            provider.setProperty(Serializable.class, key, "1");
            assertEquals(provider.toString(), "1", provider.getProperty(new S(), key));
        });
    }

    private void test(Action action) {
        String key = MetaProviderTest.class.getName() + COUNTER.incrementAndGet();
        for (MetaProvider provider : new MetaProvider[]{provider1, provider2}) {
            action.run(provider, key);
        }
    }

    private static class T {
    }

    private static class S extends T implements Serializable {
    }

    private static interface Action {

        void run(MetaProvider provider, String key);

    }

}
