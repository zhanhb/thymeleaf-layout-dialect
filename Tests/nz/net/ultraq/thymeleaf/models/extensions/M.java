/*
 * Copyright 2016 Administrator.
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
package nz.net.ultraq.thymeleaf.models.extensions;

import nz.net.ultraq.thymeleaf.internal.MetaProvider;

/**
 *
 * @author zhanhb
 */
class M implements MetaProvider {

    private final MetaProvider old, provider;

    M(MetaProvider old, MetaProvider provider) {
        this.old = old;
        this.provider = provider;
    }

    @Override
    public <T> T getProperty(Object object, String key) {
        return old.getProperty(object, key);
    }

    @Override
    public void setProperty(Object object, String key, Object value) {
        old.setProperty(object, key, value);
        provider.setProperty(object, key, value);
    }

}
