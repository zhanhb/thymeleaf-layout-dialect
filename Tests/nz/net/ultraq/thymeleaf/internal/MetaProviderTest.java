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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author zhanhb
 */
public class MetaProviderTest {

    private MetaProvider provider1, provider2;

    @Before
    public void setUp() {
        provider1 = new GroovyMetaProvider();
        provider2 = new InMemoryMetaProvider();
    }

    @Test
    public void test() {
        String key = "abc";
        String value = "123";
        test(provider1, key, value);
        test(provider2, key, value);

        test(provider1, key, null);
        test(provider2, key, null);
    }

    private void test(MetaProvider provider, String key, String value) {
        provider.setProperty(T.class, key, value);
        assertEquals(value, provider.getProperty(new T(), key));
    }

    @Test
    public void testClass() {
        String key = "test1";
        String value = "456";
        testClass(provider1, key, null);
        testClass(provider2, key, null);

        key += 1;
        testClass(provider1, key, value);
        testClass(provider2, key, value);
    }

    private void testClass(MetaProvider provider, String key, String value) {
        provider.setProperty(Class.class, key, value);
        assertEquals(value, provider.getProperty(T.class, key));
    }

    @Test
    public void testInherit() {
        String key = "test6";
        String value = "0";
        for (MetaProvider provider : new MetaProvider[]{provider1, provider2}) {
            provider.setProperty(T.class, key, value);
            provider.setProperty(Class.class, key, value + 1);
            assertEquals(provider.toString(), value + 1, provider.getProperty(S.class, key));
        }

        key += 1;
        for (MetaProvider provider : new MetaProvider[]{provider1, provider2}) {
            provider.setProperty(Class.class, key, value);
            assertEquals(provider.toString(), value, provider.getProperty(S.class, key));
        }
    }

    @Test
    public void testException() {
        for (MetaProvider provider : new MetaProvider[]{provider1, provider2}) {
            try {
                provider.getProperty(new Object(), "test7");
                fail();
            } catch (RuntimeException ex) {
                // ok
            }
        }
    }

    private static class T {
    }

    private static class S extends T {
    }

}
