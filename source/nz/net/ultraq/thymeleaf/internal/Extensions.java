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
package nz.net.ultraq.thymeleaf.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import nz.net.ultraq.thymeleaf.models.extensions.ChildModelIterator;
import nz.net.ultraq.thymeleaf.models.extensions.EventIterator;
import org.thymeleaf.DialectConfiguration;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.IProcessorDialect;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;

/**
 * Additional methods applied to the Thymeleaf class via extension programming.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 * @see IModel
 * @see TemplateModel
 * @see ITemplateEvent
 * @see IProcessableElementTag
 * @see ICloseElementTag
 * @see IStandaloneElementTag
 * @see IAttribute
 * @see IText
 * @see IExpressionContext
 */
public class Extensions {

    private static final String DIALECT_PREFIX_PREFIX = "DialectPrefix::";

    /**
     * Set that a model evaluates to 'false' if it has no events.
     *
     * @param delegate
     * @return {@code true} if this model has events.
     */
    public static boolean asBoolean(@Nullable IModel delegate) {
        return delegate != null && delegate.size() > 0;
    }

    /**
     * If this model represents an element, then this method returns an iterator
     * over any potential child items as models of their own.
     *
     * @param delegate
     * @return New model iterator.
     */
    @Nonnull
    public static Iterator<IModel> childModelIterator(@Nonnull IModel delegate) {
        return isElement(delegate) ? new ChildModelIterator(delegate) : Collections.emptyIterator();
    }

    /**
     * Iterate through each event in the model.
     *
     * @param delegate
     * @param closure
     */
    public static void each(@Nullable IModel delegate, @Nonnull ITemplateEventConsumer closure) {
        for (Iterator<ITemplateEvent> iterator = iterator(delegate); iterator.hasNext();) {
            closure.accept(iterator.next());
        }
    }

    /**
     * Compare 2 models, returning {@code true} if all of the model's events are
     * equal.
     *
     * @param delegate
     * @param other
     * @return {@code true} if this model is the same as the other one.
     */
    public static boolean equals(IModel delegate, @Nullable Object other) {
        if (other instanceof IModel) {
            IModel iModel = (IModel) other;
            if (delegate.size() == iModel.size()) {
                return everyWithIndex(delegate, (event, index) -> equals(event, iModel.get(index)));
            }
        }
        return false;
    }

    /**
     * Compare 2 models, returning {@code true} if all of the model's events
     * non-whitespace events are equal.
     *
     * @param delegate
     * @param other
     * @return {@code true} if this model is the same (barring whitespace) as
     * the other one.
     */
    public static boolean equalsIgnoreWhitespace(@Nonnull IModel delegate, @Nonnull IModel other) {
        if (other instanceof IModel) {
            Iterator<ITemplateEvent> it = iterator(delegate);
            Iterator<ITemplateEvent> iterator = iterator(other);
            ITemplateEvent next;
            ITemplateEvent that;
            do {
                do {
                    if (!it.hasNext()) {
                        do {
                            if (!iterator.hasNext()) {
                                return true;
                            }
                        } while (isWhitespace(iterator.next()));
                        return false;
                    }
                    next = it.next();
                } while (isWhitespace(next));
                do {
                    if (!iterator.hasNext()) {
                        return false;
                    }
                    that = iterator.next();
                } while (isWhitespace(that));
            } while (equals(next, that));
        }
        return false;
    }

    /**
     * Return {@code true} only if all the events in the model return
     * {@code true} for the given closure.
     *
     * @param delegate
     * @param closure
     * @return {@code true} if every event satisfies the closure.
     */
    public static boolean everyWithIndex(@Nonnull IModel delegate, @Nonnull ITemplateEventIntPredicate closure) {
        for (int i = 0, size = delegate.size(); i < size; i++) {
            if (!closure.test(delegate.get(i), i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the first event in the model that meets the criteria of the given
     * closure.
     *
     * @param delegate
     * @param closure
     * @return The first event to match the closure criteria, or {@code null} if
     * nothing matched.
     */
    @Nullable
    public static ITemplateEvent find(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        for (Iterator<ITemplateEvent> iterator = iterator(delegate); iterator.hasNext();) {
            ITemplateEvent event = iterator.next();
            if (closure.test(event)) {
                return event;
            }
        }
        return null;
    }

    /**
     * Find all events in the model that match the given closure.
     *
     * @param closure
     * @return A list of matched events.
     */
    @Nonnull
    public static List<ITemplateEvent> findAll(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        ArrayList<ITemplateEvent> answer = new ArrayList<>();
        for (Iterator<ITemplateEvent> iterator = iterator(delegate); iterator.hasNext();) {
            ITemplateEvent event = iterator.next();
            if (closure.test(event)) {
                answer.add(event);
            }
        }
        return answer;
    }

    /**
     * Returns the index of the first event in the model that meets the criteria
     * of the given closure.
     *
     * @param delegate
     * @param closure
     * @return The index of the first event to match the closure criteria, or
     * {@code -1} if nothing matched.
     */
    public static int findIndexOf(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        for (int i = 0, size = delegate.size(); i < size; i++) {
            ITemplateEvent event = delegate.get(i);
            boolean result = closure.test(event);
            if (result) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first event in the model that meets the criteria
     * of the given closure, starting from a specified position.
     *
     * @param closure
     * @return The index of the first event to match the closure criteria, or
     * {@code -1} if nothing matched.
     */
    public static int findIndexOf(@Nonnull IModel delegate, int startIndex,
            @Nonnull ITemplateEventPredicate closure) {
        for (int i = startIndex, size = delegate.size(); i < size; i++) {
            ITemplateEvent event = delegate.get(i);
            boolean result = closure.test(event);
            if (result) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A special variant of {@code findIndexOf} that uses models, as I seem to
     * be using those a lot.
     *
     * This doesn't use an equality check, but an object reference check, so if
     * a submodel is ever located from a parent (eg: any of the {@code find}
     * methods, you can use this method to find the location of that submodel
     * within the event queue.
     *
     * @param model
     * @return Index of an extracted submodel within this model.
     */
    public static int findIndexOfModel(@Nonnull IModel delegate, IModel model) {
        ITemplateEvent modelEvent = first(model);
        return findIndexOf(delegate, event -> equals(event, modelEvent));
    }

    /**
     * Returns the first instance of a model that meets the given closure
     * criteria.
     *
     * @param delegate
     * @param closure
     * @return A model over the event that matches the closure criteria, or
     * {@code null} if nothing matched.
     */
    @Nullable
    public static IModel findModel(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        return getModel(delegate, findIndexOf(delegate, closure));
    }

    /**
     * Returns the first event on the model.
     *
     * @param delegate
     * @return The model's first event.
     */
    public static ITemplateEvent first(@Nonnull IModel delegate) {
        return delegate.get(0);
    }

    /**
     * Returns the model at the given index. If the event at the index is an
     * opening element, then the returned model will consist of that element and
     * all the way through to the matching closing element.
     *
     * @param delegate
     * @param pos
     * @return Model at the given position.
     */
    @Nullable
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static IModel getModel(@Nonnull IModel delegate, int pos) {
        if (0 <= pos && pos < delegate.size()) {
            IModel clone = delegate.cloneModel();
            int removeBefore = delegate instanceof TemplateModel ? pos - 1 : pos;
            int removeAfter = clone.size() - (removeBefore + sizeOfModelAt(delegate, pos));
            while (removeBefore-- > 0) {
                removeFirst(clone);
            }
            while (removeAfter-- > 0) {
                removeLast(clone);
            }
            return clone;
        }
        return null;
    }

    /**
     * Inserts a model, creating a whitespace event before it so that it appears
     * in line with all the existing events.
     *
     * @param delegate
     * @param pos
     * @param model
     * @param modelFactory
     */
    public static void insertModelWithWhitespace(@Nonnull IModel delegate, int pos, @Nonnull IModel model, @Nonnull IModelFactory modelFactory) {

        if (0 <= pos && pos <= delegate.size()) {

            // Use existing whitespace at the insertion point
            IModel whitespace = getModel(delegate, pos);
            if (asBoolean(whitespace) && isWhitespace(whitespace)) {
                delegate.insertModel(pos, model);
                delegate.insertModel(pos, whitespace);
            } else {
                // Generate whitespace, usually inserting into a tag that is immediately
                // closed so whitespace should be added to either side
                whitespace = modelFactory.createModel(modelFactory.createText("\n\t"));
                delegate.insertModel(pos, whitespace);
                delegate.insertModel(pos, model);
                delegate.insertModel(pos, whitespace);
            }
        }
    }

    /**
     * Inserts an event, creating a whitespace event before it so that it
     * appears in line with all the existing events.
     *
     * @param delegate
     * @param pos
     * @param event
     * @param modelFactory
     */
    public static void insertWithWhitespace(@Nonnull IModel delegate, int pos, @Nonnull ITemplateEvent event, @Nonnull IModelFactory modelFactory) {

        if (0 <= pos && pos <= delegate.size()) {

            // TODO: Because I can't check the parent for whitespace hints, I should
            //       make this smarter and find whitespace within the model to copy.
            IModel whitespace = getModel(delegate, pos); // Assumes that whitespace exists at the insertion point
            if (asBoolean(whitespace) && isWhitespace(whitespace)) {
                delegate.insert(pos, event);
                delegate.insertModel(pos, whitespace);
            } else {
                IText newLine = modelFactory.createText("\n");
                if (pos == 0) {
                    delegate.insert(pos, newLine);
                    delegate.insert(pos, event);
                } else if (pos == delegate.size()) {
                    delegate.insert(pos, newLine);
                    delegate.insert(pos, event);
                    delegate.insert(pos, newLine);
                }
            }
        }
    }

    /**
     * Returns whether or not this model represents an element with potential
     * child elements.
     *
     * @param delegate
     * @return {@code true} if the first event in this model is an opening tag
     * and the last event is the matching closing tag.
     */
    public static boolean isElement(@Nonnull IModel delegate) {
        return first(delegate) instanceof IOpenElementTag && last(delegate) instanceof ICloseElementTag;
    }

    /**
     * Returns whether or not this model represents an element of the given
     * name.
     *
     * @param tagName
     * @return {@code true} if the first event in this model is an opening tag,
     * the last event is the matching closing tag, and whether the element has
     * the given tag name.
     */
    public static boolean isElementOf(@Nonnull IModel delegate, String tagName) {
        return isElement(delegate) && ((IElementTag) first(delegate)).getElementCompleteName().equals(tagName);
    }

    /**
     * Returns whether or not this model represents collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if this is a collapsible text model.
     */
    public static boolean isWhitespace(@Nonnull IModel delegate) {
        return delegate.size() == 1 && isWhitespace(first(delegate));
    }

    /**
     * Used to make this class iterable as an event queue.
     *
     * @return A new iterator over the events of this model.
     */
    public static Iterator<ITemplateEvent> iterator(IModel delegate) {
        return new EventIterator(delegate);
    }

    /**
     * Returns the last event on the model.
     *
     * @param delegate
     * @return The model's last event.
     */
    public static ITemplateEvent last(@Nonnull IModel delegate) {
        return delegate.get(delegate.size() - 1);
    }

    /**
     * If the model represents an element open to close tags, then this method
     * removes all of the inner events.
     */
    public static void removeChildren(@Nonnull IModel delegate) {
        if (isElement(delegate)) {
            while (delegate.size() > 2) {
                delegate.remove(1);
            }
        }
    }

    /**
     * Removes the first event on the model.
     *
     * @param delegate
     */
    public static void removeFirst(@Nonnull IModel delegate) {
        delegate.remove(0);
    }

    /**
     * Removes the last event on the model.
     *
     * @param delegate
     */
    public static void removeLast(@Nonnull IModel delegate) {
        delegate.remove(delegate.size() - 1);
    }

    /**
     * Removes a models-worth of events from the specified position. What this
     * means is that, if the event at the position is an opening element, then
     * it, and everything up to and including its matching end element, is
     * removed.
     *
     * @param delegate
     * @param pos
     */
    public static void removeModel(@Nonnull IModel delegate, int pos) {
        if (0 <= pos && pos < delegate.size()) {
            int modelSize = sizeOfModelAt(delegate, pos);
            while (modelSize > 0) {
                delegate.remove(pos);
                modelSize--;
            }
        }
    }

    /**
     * Replaces the model at the specified index with the given model.
     *
     * @param delegate
     * @param pos A valid index within the current model.
     * @param model
     */
    public static void replaceModel(@Nonnull IModel delegate, int pos, @Nonnull IModel model) {
        if (0 <= pos && pos < delegate.size()) {
            removeModel(delegate, pos);
            delegate.insertModel(pos, model);
        }
    }

    /**
     * If an opening element exists at the given position, this method will
     * return the 'size' of that element (number of events from here to its
     * matching closing tag).
     *
     * @param delegate
     * @param index
     * @return Size of an element from the given position, or 1 if the event at
     * the position isn't an opening element.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static int sizeOfModelAt(@Nonnull IModel delegate, int index) {
        int eventIndex = index;
        ITemplateEvent event = delegate.get(eventIndex++);

        if (event instanceof IOpenElementTag) {
            int level = 0;
            while (true) {
                event = delegate.get(eventIndex++);
                if (event instanceof IOpenElementTag) {
                    level++;
                } else if (event instanceof ICloseElementTag) {
                    if (((ICloseElementTag) event).isUnmatched()) {
                        // Do nothing.  Unmatched closing tags do not correspond to any
                        // opening element, and so should not affect the model level.
                    } else if (level == 0) {
                        break;
                    } else {
                        level--;
                    }
                }
            }
            return eventIndex - index;
        }

        return 1;
    }

    /**
     * Removes whitespace events from the head and tail of the model's
     * underlying event queue.
     *
     * @param delegate
     */
    public static void trim(@Nonnull IModel delegate) {
        while (isWhitespace(first(delegate))) {
            removeFirst(delegate);
        }
        while (isWhitespace(last(delegate))) {
            removeLast(delegate);
        }
    }

    public static boolean equals(@Nullable ITemplateEvent event, @Nullable Object other) {
        if (event instanceof IStandaloneElementTag) {
            return equals(((IStandaloneElementTag) event), other);
        } else if (event instanceof IProcessableElementTag) {
            return equals(((IProcessableElementTag) event), other);
        } else if (event instanceof ICloseElementTag) {
            return equals(((ICloseElementTag) event), other);
        } else if (event instanceof IText) {
            return equals(((IText) event), other);
        }
        return Objects.equals(event, other);
    }

    /**
     * Returns whether or not this event represents an opening element.
     *
     * @return {@code true} if this event is an opening tag.
     */
    public static boolean isClosingElement(@Nullable ITemplateEvent delegate) {
        return delegate instanceof ICloseElementTag || delegate instanceof IStandaloneElementTag;
    }

    /**
     * Returns whether or not this event represents a closing element of the
     * given name.
     *
     * @param tagName
     * @return {@code true} if this event is a closing tag and has the given tag
     * name.
     */
    @SuppressWarnings("null")
    public static boolean isClosingElementOf(@Nullable ITemplateEvent delegate, String tagName) {
        return isClosingElement(delegate) && ((IElementTag) delegate).getElementCompleteName().equals(tagName);
    }

    /**
     * Returns whether or not this event represents an opening element.
     *
     * @return {@code true} if this event is an opening tag.
     */
    public static boolean isOpeningElement(@Nullable ITemplateEvent delegate) {
        return delegate instanceof IOpenElementTag || delegate instanceof IStandaloneElementTag;
    }

    /**
     * Returns whether or not this event represents an opening element of the
     * given name.
     *
     * @param tagName
     * @return {@code true} if this event is an opening tag and has the given
     * tag name.
     */
    @SuppressWarnings("null")
    public static boolean isOpeningElementOf(@Nullable ITemplateEvent delegate, String tagName) {
        return isOpeningElement(delegate) && ((IElementTag) delegate).getElementCompleteName().equals(tagName);
    }

    /**
     * Returns whether or not this event represents collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if this is a collapsible text node.
     */
    public static boolean isWhitespace(@Nonnull ITemplateEvent delegate) {
        return delegate instanceof IText && isWhitespace((IText) delegate);
    }

    /**
     * Compares this open tag with another.
     *
     * @param delegate
     * @param other
     * @return {@code true} if this tag has the same name and attributes as the
     * other element.
     */
    public static boolean equals(IProcessableElementTag delegate, @Nullable Object other) {
        return other instanceof IProcessableElementTag
                && Objects.equals(delegate.getElementCompleteName(), ((IElementTag) other).getElementCompleteName())
                && Objects.equals(delegate.getAttributeMap(), ((IProcessableElementTag) other).getAttributeMap());
    }

    /**
     * Compares this close tag with another.
     *
     * @param delegate
     * @param other
     * @return {@code true} if this tag has the same name as the other element.
     */
    public static boolean equals(ICloseElementTag delegate, @Nullable Object other) {
        return other instanceof ICloseElementTag
                && Objects.equals(delegate.getElementCompleteName(), ((IElementTag) other).getElementCompleteName());
    }

    /**
     * Compares this standalone tag with another.
     *
     * @param delegate
     * @param other
     * @return {@code true} if this tag has the same name and attributes as the
     * other element.
     */
    public static boolean equals(IStandaloneElementTag delegate, @Nullable Object other) {
        return other instanceof IStandaloneElementTag
                && Objects.equals(delegate.getElementCompleteName(), ((IElementTag) other).getElementCompleteName())
                && Objects.equals(delegate.getAttributeMap(), ((IProcessableElementTag) other).getAttributeMap());
    }

    /**
     * Returns whether or not an attribute is an attribute processor of the
     * given name, checks both prefix:processor and data-prefix-processor
     * variants.
     *
     * @param delegate
     * @param prefix
     * @param name
     * @return {@code true} if this attribute is an attribute processor of the
     * matching name.
     */
    public static boolean equalsName(@Nonnull IAttribute delegate, @Nonnull String prefix, @Nonnull String name) {
        String attributeName = delegate.getAttributeCompleteName();
        return (prefix + ":" + name).equals(attributeName) || ("data-" + prefix + "-" + name).equals(attributeName);
    }

    /**
     * Compares this text with another.
     *
     * @param delegate
     * @param other
     * @return {@code true} if the text content matches.
     */
    public static boolean equals(IText delegate, @Nullable Object other) {
        return other instanceof IText && Objects.equals(delegate.getText(), ((IText) other).getText());
    }

    /**
     * Returns whether or not this text event is collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if, when trimmed, the text content is empty.
     */
    public static boolean isWhitespace(@Nonnull IText delegate) {
        return delegate.getText().trim().isEmpty();
    }

    /**
     * Retrieves an item from the context, or creates one on the context if it
     * doesn't yet exist.
     *
     * @param <T>
     * @param delegate
     * @param key
     * @param closure
     * @return The item cached on the context through the given key, or first
     * constructed through the closure.
     */
    public static <T> T getOrCreate(@Nonnull IContext delegate, @Nonnull String key, Supplier<T> closure) {
        if (delegate instanceof IEngineContext) {
            return getOrCreate((IEngineContext) delegate, key, closure);
        }

        ConcurrentMap<String, T> dialectPrefixCache = DialectPrefixCacheHolder.getDialectPrefixCache(delegate);

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
     * @param delegate
     * @param dialectClass
     * @return The configured prefix for the dialect, or {@code null} if the
     * dialect being queried hasn't been configured.
     */
    public static String getPrefixForDialect(@Nonnull IExpressionContext delegate, Class<? extends IProcessorDialect> dialectClass) {
        return getOrCreate(delegate, DIALECT_PREFIX_PREFIX + dialectClass.getName(), () -> {
            DialectConfiguration dialectConfiguration = null;
            for (DialectConfiguration dialectConfig : delegate.getConfiguration().getDialectConfigurations()) {
                if (dialectClass.isInstance(dialectConfig.getDialect())) {
                    dialectConfiguration = dialectConfig;
                    break;
                }
            }
            if (dialectConfiguration != null) {
                if (dialectConfiguration.isPrefixSpecified()) {
                    return dialectConfiguration.getPrefix();
                } else {
                    return ((IProcessorDialect) dialectConfiguration.getDialect()).getPrefix();
                }
            }
            return null;
        });
    }

    private Extensions() {
        throw new AssertionError();
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
