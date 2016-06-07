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

import lombok.SneakyThrows;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteArray;

/**
 *
 * @author zhanhb
 */
class GroovyMetaProvider extends MetaProvider {

    private static final CallSite[] callSites
            = new CallSiteArray(GroovyMetaProvider.class,
                    new String[]{"getAt", "putAt", "metaClass"}).array;

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T getProperty(Object object, String key) {
        CallSite[] var3 = callSites;
        return (T) var3[0].call(object, key);
    }

    @Override
    @SneakyThrows
    public void setProperty(Object object, String key, Object value) {
        CallSite[] var4 = callSites;
        var4[1].call(var4[2].callGetProperty(object), key, value);
    }

}
