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

/**
 *
 * @author zhanhb
 */
public abstract class MetaProvider {

    public static final MetaProvider INSTANCE = createInstance();

    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
    private static MetaProvider createInstance() {
        try {
            Class<?> cl = groovy.lang.GroovyObject.class;
            Class.forName(cl.getName(), true, cl.getClassLoader());
            return new GroovyMetaProvider();
        } catch (Throwable ex) {
            return new InMemoryMetaProvider();
        }
    }

    public abstract <T> T getProperty(Object object, String key);

    public abstract void setProperty(Object object, String key, Object value);

}
