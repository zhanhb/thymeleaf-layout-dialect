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
package nz.net.ultraq.thymeleaf.models;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import nz.net.ultraq.thymeleaf.internal.ITemplateEventConsumer;
import nz.net.ultraq.thymeleaf.internal.ITemplateEventIntPredicate;
import nz.net.ultraq.thymeleaf.internal.ITemplateEventPredicate;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.MetaProvider;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;

/**
 *
 * @author zhanhb
 */
public class ModelExtensions {

    static {
        try {
            Field field = MetaProvider.class.getField("INSTANCE");
            setStaticFinalField(field, new M(MetaProvider.INSTANCE, new GroovyMetaProvider()));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void apply() {
        IModel.metaClass {
            asBoolean << {
                MetaClass.asBoolean(delegate)
            }
            childEventIterator << {
                MetaClass.childEventIterator(delegate)
            }
            childModelIterator << {
                MetaClass.childModelIterator(delegate)
            }
            clear << {
                MetaClass.clear(delegate)
            }
            clearChildren << {
                MetaClass.clearChildren(delegate)
            }
            each << { Closure closure ->
                MetaClass.each(delegate, closure as ITemplateEventConsumer)
            }
            equals << { Object other ->
                MetaClass.equals(delegate, other)
            }
            equalsIgnoreWhitespace << { IModel other ->
                MetaClass.equalsIgnoreWhitespace(delegate, other)
            }
            everyWithIndex << { Closure closure ->
                MetaClass.everyWithIndex(delegate, closure as ITemplateEventIntPredicate)
            }
            find << { Closure closure ->
                MetaClass.find(delegate, closure as ITemplateEventPredicate)
            }
            findModel << { Closure closure ->
                MetaClass.findModel(delegate, closure as ITemplateEventPredicate)
            }
            findWithIndex << { Closure closure ->
                MetaClass.findWithIndex(delegate, closure as ITemplateEventIntPredicate)
            }
            first << {
                MetaClass.first(delegate)
            }
            getModel << { int pos ->
                MetaClass.getModel(delegate, pos)
            }
            insertModelWithWhitespace << { int pos, IModel model ->
                MetaClass.insertModelWithWhitespace(delegate, pos, model)
            }
            insertWithWhitespace << { int pos, ITemplateEvent event, IModelFactory modelFactory ->
                MetaClass.insertWithWhitespace(delegate, pos, event, modelFactory)
            }
            isElement << {
                MetaClass.isElement(delegate)
            }
            isWhitespace << {
                MetaClass.isWhitespace(delegate)
            }
            last << {
                MetaClass.last(delegate)
            }
            removeFirst << {
                MetaClass.removeFirst(delegate)
            }
            removeLast << {
                MetaClass.removeLast(delegate)
            }
            removeModel << { int pos ->
                MetaClass.removeModel(delegate, pos)
            }
            removeModelWithWhitespace << { int pos ->
                MetaClass.removeModelWithWhitespace(delegate, pos)
            }
            replaceModel << { int pos, IModel model ->
                MetaClass.replaceModel(delegate, pos, model)
            }
        }
        TemplateModel.metaClass {
            getTemplate << {
                MetaClass.getTemplate(delegate)
            }
        }
        ITemplateEvent.metaClass {
            isWhitespace << {
                MetaClass.isWhitespace(delegate)
            }
        }
        IOpenElementTag.metaClass {
            equals << { Object other ->
                MetaClass.equals(delegate, other)
            }
        }
        ICloseElementTag.metaClass {
            equals << { Object other ->
                MetaClass.equals(delegate, other)
            }
        }
        IStandaloneElementTag.metaClass {
            equals << { Object other ->
                MetaClass.equals(delegate, other)
            }
        }
        IAttribute.metaClass {
            equalsName << { String prefix, String name ->
                MetaClass.equalsName(delegate, prefix, name)
            }
            getAttributeName << {
                MetaClass.getAttributeName(delegate)
            }
        }
        IText.metaClass {
            equals << { Object other ->
                MetaClass.equals(delegate, other)
            }
            isWhitespace << {
                MetaClass.isWhitespace(delegate)
            }
        }
    }

    private static void setStaticFinalField(Field field, Object obj) throws Throwable {
        Field field1 = Field.class.getDeclaredField("modifiers");
        field1.setAccessible(true);
        field1.set(field, field.getModifiers() & ~Modifier.FINAL);
        field.setAccessible(true);
        field.set(field, obj);
    }

    private static class M implements MetaProvider {

        private final MetaProvider old;
        private final MetaProvider provider;

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

}
