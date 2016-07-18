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

import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import nz.net.ultraq.thymeleaf.models.extensions.ChildEventIterator;
import nz.net.ultraq.thymeleaf.models.extensions.ChildModelIterator;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.HTMLElementDefinition;
import org.thymeleaf.engine.HTMLElementType;
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
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Additional methods applied to the Thymeleaf {@link IModel} class via Groovy
 * meta-programming.
 *
 * @author zhanhb
 */
public class MetaClass {

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
     * over those child events.
     *
     * @param delegate
     * @return An iterator over this model's child events, or an empty iterator
     * for all other model types.
     */
    @Nullable
    public static Iterator<ITemplateEvent> childEventIterator(@Nonnull IModel delegate) {
        return isElement(delegate) ? new ChildEventIterator(delegate) : null;
    }

    /**
     * If this model represents an element, then this method returns an iterator
     * over any potential child items as models of their own.
     *
     * @param delegate
     * @return New model iterator.
     */
    @Nullable
    public static Iterator<IModel> childModelIterator(@Nonnull IModel delegate) {
        return isElement(delegate) ? new ChildModelIterator(delegate) : null;
    }

    /**
     * If the model represents an element open to close tags, then this method
     * removes all of the inner events. Otherwise, it does nothing.
     *
     * @param delegate
     */
    public static void clearChildren(@Nonnull IModel delegate) {
        if (isElement(delegate)) {
            while (delegate.size() > 2) {
                delegate.remove(1);
            }
        }
    }

    /**
     * Iterate through each event in the model.
     *
     * @param delegate
     * @param closure
     */
    public static void each(@Nullable IModel delegate, @Nonnull ITemplateEventConsumer closure) {
        if (delegate != null) {
            for (int i = 0; i < delegate.size(); i++) {
                closure.accept(delegate.get(i));
            }
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
                return everyWithIndex(delegate, (event, index) -> {
                    return equals(event, iModel.get(index));
                });
            }
        }
        return false;
    }

    private static boolean equals(@Nullable ITemplateEvent event, Object other) {
        if (event instanceof IOpenElementTag) {
            return equals(((IOpenElementTag) event), other);
        } else if (event instanceof ICloseElementTag) {
            return equals(((ICloseElementTag) event), other);
        } else if (event instanceof IStandaloneElementTag) {
            return equals(((IStandaloneElementTag) event), other);
        } else if (event instanceof IText) {
            return equals(((IText) event), other);
        }
        return Objects.equals(event, other);
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
    public static boolean equalsIgnoreWhitespace(IModel delegate, IModel other) {
        int thisEventIndex = 0;
        int otherEventIndex = 0;

        while (thisEventIndex < delegate.size() || otherEventIndex < other.size()) {
            ITemplateEvent thisEvent = delegate.get(thisEventIndex);
            ITemplateEvent otherEvent = other.get(otherEventIndex);
            if (isWhitespace(thisEvent)) {
                thisEventIndex++;
                continue;
            } else if (isWhitespace(otherEvent)) {
                otherEventIndex++;
                continue;
            }
            if (!equals(thisEvent, otherEvent)) {
                return false;
            }
            thisEventIndex++;
            otherEventIndex++;
        }

        return thisEventIndex == delegate.size() && otherEventIndex == other.size();

    }

    /**
     * Return {@code true} only if all the events in the model return
     * {@code true} for the given closure.
     *
     * @param delegate
     * @param closure
     * @return {@code true} if every event satisfies the closure.
     */
    public static boolean everyWithIndex(IModel delegate, @Nonnull ITemplateEventIntPredicate closure) {
        for (int i = 0; i < delegate.size(); i++) {
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
     * Models returned via this method are also aware of their position in the
     * event queue of the parent model, accessible via their {@code index}
     * property.
     *
     * @param delegate
     * @param closure
     * @return The first event to match the closure criteria, or {@code null} if
     * nothing matched.
     */
    @Nullable
    public static ITemplateEvent find(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        for (int i = 0; i < delegate.size(); i++) {
            ITemplateEvent event = delegate.get(i);
            boolean result = closure.test(event);
            if (result) {
                MetaProvider.INSTANCE.setProperty(event, "index", i);
                return event;
            }
        }
        return null;
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
    public static int findIndexOf(IModel delegate, ITemplateEventPredicate closure) {
        for (int i = 0; i < delegate.size(); i++) {
            ITemplateEvent event = delegate.get(i);
            boolean result = closure.test(event);
            if (result) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the first instance of a model that meets the given closure
     * criteria.
     *
     * Models returned via this method are also aware of their position in the
     * event queue of the parent model, accessible via their {@code index}
     * property.
     *
     * @param delegate
     * @param closure
     * @return A model over the event that matches the closure criteria, or
     * {@code null} if nothing matched.
     */
    @Nullable
    public static IModel findModel(@Nonnull IModel delegate, @Nonnull ITemplateEventPredicate closure) {
        int eventIndex = findIndexOf(delegate, closure);
        if (eventIndex != -1) {
            IModel model = getModel(delegate, eventIndex);
            MetaProvider.INSTANCE.setProperty(model, "startIndex", eventIndex);
            MetaProvider.INSTANCE.setProperty(model, "endIndex", eventIndex + model.size());
            return model;
        }
        return null;
    }

    /**
     * Returns the first event on the model.
     *
     * @param delegate
     * @return The model's first event, or {@code null} if the model has no
     * events.
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
    @Nonnull
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static IModel getModel(@Nonnull IModel delegate, int pos) {
        int modelSize = calculateModelSize(delegate, pos);
        IModel subModel = delegate.cloneModel();
        int removeBefore = delegate instanceof TemplateModel ? pos - 1 : pos;
        int removeAfter = subModel.size() - (removeBefore + modelSize);
        while (removeBefore-- > 0) {
            removeFirst(subModel);
        }
        while (removeAfter-- > 0) {
            removeLast(subModel);
        }
        return subModel;
    }

    /**
     * Inserts a model, creating a whitespace event before it so that it appears
     * in line with all the existing events.
     *
     * @param delegate
     * @param pos
     * @param model
     */
    public static void insertModelWithWhitespace(@Nonnull IModel delegate, int pos, IModel model) {
        IModel whitespace = getModel(delegate, pos); // Assumes that whitespace exists at the insertion point
        if (isWhitespace(whitespace)) {
            delegate.insertModel(pos, model);
            delegate.insertModel(pos, whitespace);
        } else {
            delegate.insertModel(pos, model);
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
    public static void insertWithWhitespace(IModel delegate, int pos, ITemplateEvent event, IModelFactory modelFactory) {
        // TODO: Because I can't check the parent for whitespace hints, I should
        //       make this smarter and find whitespace within the model to copy.
        IModel whitespace = getModel(delegate, pos); // Assumes that whitespace exists at the insertion point
        if (isWhitespace(whitespace)) {
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
     * Returns whether or not this model represents collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if this is a collapsible text model.
     */
    public static boolean isWhitespace(@Nonnull IModel delegate) {
        return delegate.size() == 1 && isWhitespace(first(delegate));
    }

    /**
     * Returns the last event on the model.
     *
     * @param delegate
     * @return The model's lats event, or {@code null} if the model has no
     * events.
     */
    public static ITemplateEvent last(@Nonnull IModel delegate) {
        return delegate.get(delegate.size() - 1);
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
        int modelSize = calculateModelSize(delegate, pos);
        while (modelSize > 0) {
            delegate.remove(pos);
            modelSize--;
        }
    }

    /**
     * Removes a models-worth of events from the specified position, plus the
     * preceeding whitespace event if any.
     *
     * @param delegate
     * @param pos
     */
    public static void removeModelWithWhitespace(@Nonnull IModel delegate, int pos) {
        removeModel(delegate, pos);
        ITemplateEvent priorEvent = delegate.get(pos - 1);
        if (isWhitespace(priorEvent)) {
            delegate.remove(pos - 1);
        }
    }

    /**
     * Replaces the model at the specified index with the given model.
     *
     * @param delegate
     * @param pos
     * @param model
     */
    public static void replaceModel(@Nonnull IModel delegate, int pos, IModel model) {
        removeModel(delegate, pos);
        delegate.insertModel(pos, model);
    }

    /**
     * Shortcut to the template name found on the template data object. Only
     * works if the template was resolved via a name, rather than a string (eg:
     * anonymous template), in which case this can return the entire template!
     *
     * @param delegate
     * @return Template name.
     */
    public static String getTemplate(@Nonnull TemplateModel delegate) {
        return delegate.getTemplateData().getTemplate();
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
    public static boolean equals(IOpenElementTag delegate, @Nullable Object other) {
        return other instanceof IOpenElementTag
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
    public static boolean equalsName(@Nonnull IAttribute delegate, String prefix, String name) {
        String attributeName = delegate.getAttributeCompleteName();
        return (prefix + ":" + name).equals(attributeName) || ("data-" + prefix + "-" + name).equals(attributeName);
    }

    /**
     * Shortcut to the attribute name class on the attribute definition.
     *
     * @param delegate
     * @return Attribute name object.
     */
    public static AttributeName getAttributeName(@Nonnull IAttribute delegate) {
        return delegate.getAttributeDefinition().getAttributeName();
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
     * If an opening element exists at the given position, this method will
     * return the 'size' of that element (number of events from here to its
     * matching closing tag). Otherwise, a size of 1 is returned.
     *
     * @param model
     * @param index
     * @return Size of an element from the given position, or 1 if the event at
     * the position isn't an opening element.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    private static int calculateModelSize(@Nonnull IModel model, int index) {
        int eventIndex = index;
        ITemplateEvent event = model.get(eventIndex++);

        if (event instanceof IOpenElementTag) {
            int level = 0;
            while (true) {
                event = model.get(eventIndex++);
                if (event instanceof IOpenElementTag) {
                    level++;
                }
                if (event instanceof ICloseElementTag) {
                    ICloseElementTag tag = (ICloseElementTag) event;
                    if (tag.getTemplateMode() == TemplateMode.HTML && ((HTMLElementDefinition) tag.getElementDefinition()).getType() == HTMLElementType.VOID) {
                        // Do nothing.  This is to capture closing tags for HTML void
                        // elements which shouldn't be specified according to the HTML spec.
                        // https://html.spec.whatwg.org/multipage/syntax.html#void-elements
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

    private MetaClass() {
        throw new AssertionError();
    }

}
