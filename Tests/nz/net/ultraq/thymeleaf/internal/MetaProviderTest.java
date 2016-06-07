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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author zhanhb
 */
public class MetaProviderTest {

    @Test
    public void test() {
        String key = "abc";
        String value = "123";
        test(MetaProvider.INSTANCE, key, value);
        test(new InMemoryMetaProvider(), key, value);

        test(MetaProvider.INSTANCE, key, null);
        test(new InMemoryMetaProvider(), key, null);
    }

    private void test(MetaProvider provider, String key, String value) {
        provider.setProperty(T.class, key, value);
        assertEquals(value, provider.getProperty(new T(), key));
    }

    @Ignore
    @Test
    public void testClass() {
        String key = "test1";
        String value = "456";
        InMemoryMetaProvider provider2 = new InMemoryMetaProvider();
        testClass(MetaProvider.INSTANCE, key, null);
        testClass(provider2, key, null);

        testClass(MetaProvider.INSTANCE, key, value);
        testClass(provider2, key, value);
    }

    private void testClass(MetaProvider provider, String key, String value) {
        provider.setProperty(Class.class, key, value);
        assertEquals(value, provider.getProperty(T.class, key));
    }

    private static class T {
    }

}
