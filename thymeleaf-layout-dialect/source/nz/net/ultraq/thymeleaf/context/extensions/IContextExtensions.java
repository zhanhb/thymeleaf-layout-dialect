/*
 * Copyright 2016, Emanuel Rabina (http://www.ultraq.net.nz/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.net.ultraq.thymeleaf.context.extensions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import nz.net.ultraq.thymeleaf.internal.Supplier;
import org.thymeleaf.DialectConfiguration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.IProcessorDialect;

/**
 * Meta-programming extensions to the {@link IContext} class.
 *
 * @author Emanuel Rabina
 */
public class IContextExtensions {

    private static final String DIALECT_PREFIX_PREFIX = "DialectPrefix::";

    /**
     * Enables use of the {@code value = context[key]} syntax over the context
     * object, is a synonym for the {@code getVariable} method.
     *
     * @param self
     * @param name Name of the variable on the context to retrieve.
     * @return The variable value, or {@code null} if the variable isn't mapped
     * to anything on the context.
     */
    public static Object getAt(IContext self, String name) {
        return self.getVariable(name);
    }

    /**
     * Retrieves an item from the context, or creates one on the context if it
     * doesn't yet exist.
     *
     * @param <T>
     * @param self
     * @param key
     * @param closure
     * @return The item cached on the context through the given key, or first
     * constructed through the closure.
     */
    public static <T> T getOrCreate(@Nonnull IContext self, @Nonnull String key, Supplier<T> closure) {
        if (self instanceof IEngineContext) {
            return getOrCreate((IEngineContext) self, key, closure);
        }

        ConcurrentMap<String, T> dialectPrefixCache = DialectPrefixCacheHolder.getDialectPrefixCache(self);

        T value = dialectPrefixCache.get(key);
        if (value == null) {
            value = closure.get();
            if (value != null) {
                dialectPrefixCache.putIfAbsent(key, value);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getOrCreate(IEngineContext delegate, @Nonnull String key, Supplier<T> closure) {
        Object value = delegate.getVariable(key);
        if (value == null) {
            value = closure.get();
            delegate.setVariable(key, value);
        }
        return (T) value;
    }

    /**
     * Returns the configured prefix for the given dialect. If the dialect
     * prefix has not been configured.
     *
     * @param self
     * @param dialectClass
     * @return The configured prefix for the dialect, or {@code null} if the
     * dialect being queried hasn't been configured.
     */
    public static String getPrefixForDialect(@Nonnull IExpressionContext self, Class<? extends IProcessorDialect> dialectClass) {
        return getOrCreate(self, DIALECT_PREFIX_PREFIX + dialectClass.getName(), () -> {
            for (DialectConfiguration dialectConfig : self.getConfiguration().getDialectConfigurations()) {
                if (dialectClass.isInstance(dialectConfig.getDialect())) {
                    if (dialectConfig.isPrefixSpecified()) {
                        return dialectConfig.getPrefix();
                    }
                    return ((IProcessorDialect) dialectConfig.getDialect()).getPrefix();
                }
            }
            return null;
        });
    }

    /**
     * Enables use of the {@code context[key] = value} syntax over the context
     * object, is a synonym for the {@code setVariable} method.
     *
     * @param self
     * @param name Name of the variable to map the value to.
     * @param value The value to set.
     */
    public static void putAt(IContext self, String name, Object value) {
        if (self instanceof IEngineContext) {
            ((IEngineContext) self).setVariable(name, value);
        } else {
            DialectPrefixCacheHolder.getDialectPrefixCache(self).put(name, value);
        }
    }

    @SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "NestedAssignment", "rawtypes", "unchecked"})
    private static class DialectPrefixCacheHolder {

        private static final ConcurrentWeakIdentityHashMap<IContext, ConcurrentMap<String, Object>> CACHE
                = new ConcurrentWeakIdentityHashMap<>(20);

        static <T> ConcurrentMap<String, T> getDialectPrefixCache(IContext delegate) {
            ConcurrentMap dialectPrefixCache, newCache;
            return (dialectPrefixCache = CACHE.get(delegate)) == null
                    && (dialectPrefixCache = CACHE.putIfAbsent(delegate,
                            newCache = new ConcurrentHashMap<>(4))) == null
                            ? newCache : dialectPrefixCache;
        }

    }

}
