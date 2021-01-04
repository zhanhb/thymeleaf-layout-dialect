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
package nz.net.ultraq.thymeleaf.models.extensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import nz.net.ultraq.thymeleaf.internal.Consumer;
import nz.net.ultraq.thymeleaf.internal.BiPredicate;
import nz.net.ultraq.thymeleaf.internal.Predicate;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;

/**
 * Meta-programming extensions to the {@link IModel} class.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class IModelExtensions {

    /**
     * Set that a model evaluates to 'false' if it has no events.
     *
     * @param self
     * @return {@code true} if this model has events.
     */
    public static boolean asBoolean(@Nullable IModel self) {
        return self != null && self.size() > 0;
    }

    /**
     * If this model represents an element, then this method returns an iterator
     * over any potential child items as models of their own.
     *
     * @param self
     * @return New model iterator.
     */
    @Nullable
    public static ChildModelIterator childModelIterator(@Nonnull IModel self) {
        return isElement(self) ? new ChildModelIterator(self) : null;
    }

    /**
     * Iterate through each event in the model.
     *
     * @param self
     * @param closure
     */
    public static void each(@Nullable IModel self, @Nonnull Consumer<ITemplateEvent> closure) {
        Iterator<ITemplateEvent> it = maskNull(self);
        while (it.hasNext()) {
            closure.accept(it.next());
        }
    }

    /**
     * Compare 2 models, returning {@code true} if all of the model's events are
     * equal.
     *
     * @param self
     * @param other
     * @return {@code true} if this model is the same as the other one.
     */
    public static boolean equals(IModel self, Object other) {
        if (self != null && other instanceof IModel) {
            IModel iModel = (IModel) other;
            if (self.size() == iModel.size()) {
                return everyWithIndex(self, (event, index) -> ITemplateEventExtensions.equals(event, iModel.get(index)));
            }
        }
        return false;
    }

    /**
     * Compare 2 models, returning {@code true} if all of the model's events
     * non-whitespace events are equal.
     *
     * @param self
     * @param other
     * @return {@code true} if this model is the same (barring whitespace) as
     * the other one.
     */
    public static boolean equalsIgnoreWhitespace(@Nullable IModel self, @Nullable IModel other) {
        Iterator<ITemplateEvent> it = maskNull(self);
        Iterator<ITemplateEvent> iterator = maskNull(other);
        ITemplateEvent next;
        ITemplateEvent that;
        do {
            do {
                if (!it.hasNext()) {
                    do {
                        if (!iterator.hasNext()) {
                            return true;
                        }
                    } while (ITemplateEventExtensions.isWhitespace(iterator.next()));
                    return false;
                }
                next = it.next();
            } while (ITemplateEventExtensions.isWhitespace(next));
            do {
                if (!iterator.hasNext()) {
                    return false;
                }
                that = iterator.next();
            } while (ITemplateEventExtensions.isWhitespace(that));
        } while (ITemplateEventExtensions.equals(next, that));
        return false;
    }

    /**
     * Return {@code true} only if all the events in the model return
     * {@code true} for the given closure.
     *
     * @param self
     * @param closure
     * @return {@code true} if every event satisfies the closure.
     */
    public static boolean everyWithIndex(@Nullable IModel self, @Nonnull BiPredicate<ITemplateEvent, Integer> closure) {
        int index = 0;
        for (Iterator<ITemplateEvent> it = maskNull(self); it.hasNext(); ++index) {
            if (!closure.test(it.next(), index)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the first event in the model that meets the criteria of the given
     * closure.
     *
     * @param self
     * @param closure
     * @return The first event to match the closure criteria, or {@code null} if
     * nothing matched.
     */
    @Nullable
    public static ITemplateEvent find(@Nullable IModel self, @Nonnull Predicate<ITemplateEvent> closure) {
        for (Iterator<ITemplateEvent> it = maskNull(self); it.hasNext();) {
            ITemplateEvent event = it.next();
            if (closure.test(event)) {
                return event;
            }
        }
        return null;
    }

    /**
     * Find all events in the model that match the given closure.
     *
     * @param self
     * @param closure
     * @return A list of matched events.
     */
    @Nonnull
    public static List<ITemplateEvent> findAll(@Nullable IModel self, @Nonnull Predicate<ITemplateEvent> closure) {
        @SuppressWarnings("CollectionWithoutInitialCapacity")
        ArrayList<ITemplateEvent> answer = new ArrayList<>();
        for (Iterator<ITemplateEvent> it = maskNull(self); it.hasNext();) {
            ITemplateEvent event = it.next();
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
     * @param self
     * @param closure
     * @return The index of the first event to match the closure criteria, or
     * {@code -1} if nothing matched.
     */
    public static int findIndexOf(@Nonnull IModel self, @Nonnull Predicate<ITemplateEvent> closure) {
        for (int i = 0, size = self.size(); i < size; i++) {
            ITemplateEvent event = self.get(i);
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
     * @param self
     * @param closure
     * @return The index of the first event to match the closure criteria, or
     * {@code -1} if nothing matched.
     */
    public static int findIndexOf(@Nonnull IModel self, int startIndex,
            @Nonnull Predicate<ITemplateEvent> closure) {
        for (int i = startIndex, size = self.size(); i < size; i++) {
            ITemplateEvent event = self.get(i);
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
     * @param self
     * @param model
     * @return Index of an extracted submodel within this model.
     */
    public static int findIndexOfModel(@Nonnull IModel self, IModel model) {
        ITemplateEvent modelEvent = first(model);
        return findIndexOf(self, event -> ITemplateEventExtensions.equals(event, modelEvent));
    }

    /**
     * Returns the first instance of a model that meets the given closure
     * criteria.
     *
     * @param self
     * @param closure
     * @return A model over the event that matches the closure criteria, or
     * {@code null} if nothing matched.
     */
    public static IModel findModel(@Nonnull IModel self, @Nonnull Predicate<ITemplateEvent> closure) {
        return getModel(self, findIndexOf(self, closure));
    }

    /**
     * Returns the first event on the model.
     *
     * @param self
     * @return The model's first event.
     */
    public static ITemplateEvent first(@Nonnull IModel self) {
        return self.get(0);
    }

    /**
     * Returns the model at the given index. If the event at the index is an
     * opening element, then the returned model will consist of that element and
     * all the way through to the matching closing element.
     *
     * @param self
     * @param pos A valid index within the current model.
     * @return Model at the given position, or `null` if the position is outside
     * of the event queue.
     */
    @Nullable
    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "AssignmentToMethodParameter"})
    public static IModel getModel(@Nonnull IModel self, int pos) {
        if (0 <= pos && pos < self.size()) {
            IModel result = self.getConfiguration()
                    .getModelFactory(self.getTemplateMode())
                    .createModel();
            for (int size = sizeOfModelAt(self, pos); size-- > 0; ++pos) {
                result.add(self.get(pos));
            }
            return result;
        }
        return null;
    }

    /**
     * Inserts a model, creating a whitespace event before it so that it appears
     * in line with all the existing events.
     *
     * @param self
     * @param pos A valid index within the current model.
     * @param model
     * @param modelFactory
     */
    @SuppressWarnings("null")
    public static void insertModelWithWhitespace(@Nonnull IModel self, int pos,
            @Nullable IModel model, @Nonnull IModelFactory modelFactory) {

        if (0 <= pos && pos <= self.size()) {

            // Use existing whitespace found at or before the insertion point
            IModel whitespace = getModel(self, pos);
            if (asBoolean(whitespace) && isWhitespace(whitespace)) {
                self.insertModel(pos, model);
                self.insertModel(pos, whitespace);
                return;
            }
            if (pos > 0) {
                whitespace = getModel(self, pos - 1);
                if (asBoolean(whitespace) && isWhitespace(whitespace)) {
                    self.insertModel(pos, whitespace);
                    self.insertModel(pos, model);
                    return;
                }
            }

            // Generate whitespace, usually inserting into a tag that is immediately
            // closed so whitespace should be added to either side
            whitespace = modelFactory.createModel(modelFactory.createText("\n\t"));
            self.insertModel(pos, whitespace);
            self.insertModel(pos, model);
            self.insertModel(pos, whitespace);
        }
    }

    /**
     * Inserts an event, creating a whitespace event before it so that it
     * appears in line with all the existing events.
     *
     * @param self
     * @param pos A valid index within the current model.
     * @param event
     * @param modelFactory
     */
    @SuppressWarnings("null")
    public static void insertWithWhitespace(@Nonnull IModel self, int pos,
            @Nullable ITemplateEvent event, @Nonnull IModelFactory modelFactory) {

        if (0 <= pos && pos <= self.size()) {

            // TODO: Because I can't check the parent for whitespace hints, I should
            //       make this smarter and find whitespace within the model to copy.
            IModel whitespace = getModel(self, pos); // Assumes that whitespace exists at the insertion point
            if (asBoolean(whitespace) && isWhitespace(whitespace)) {
                self.insert(pos, event);
                self.insertModel(pos, whitespace);
            } else {
                IText newLine = modelFactory.createText("\n");
                if (pos == 0) {
                    self.insert(pos, newLine);
                    self.insert(pos, event);
                } else if (pos == self.size()) {
                    self.insert(pos, newLine);
                    self.insert(pos, event);
                    self.insert(pos, newLine);
                }
            }
        }
    }

    /**
     * Returns whether or not this model represents an element with potential
     * child elements.
     *
     * @param self
     * @return {@code true} if the first event in this model is an opening tag
     * and the last event is the matching closing tag.
     */
    public static boolean isElement(@Nonnull IModel self) {
        return first(self) instanceof IOpenElementTag && last(self) instanceof ICloseElementTag;
    }

    /**
     * Returns whether or not this model represents an element of the given
     * name.
     *
     * @param self
     * @param tagName
     * @return {@code true} if the first event in this model is an opening tag,
     * the last event is the matching closing tag, and whether the element has
     * the given tag name.
     */
    public static boolean isElementOf(@Nonnull IModel self, String tagName) {
        return isElement(self) && Objects.equals(((IElementTag) first(self)).getElementCompleteName(), tagName);
    }

    /**
     * Returns whether or not this model represents collapsible whitespace.
     *
     * @param self
     * @return {@code true} if this is a collapsible text model.
     */
    public static boolean isWhitespace(@Nonnull IModel self) {
        return self.size() == 1 && ITemplateEventExtensions.isWhitespace(first(self));
    }

    /**
     * Used to make this class iterable as an event queue.
     *
     * @param self
     * @return A new iterator over the events of this model.
     */
    @Nonnull
    public static EventIterator iterator(@Nonnull IModel self) {
        return new EventIterator(self);
    }

    private static Iterator<ITemplateEvent> maskNull(@Nullable IModel self) {
        Iterator<ITemplateEvent> result = self != null ? iterator(self) : null;
        return result != null ? result : Collections.emptyIterator();
    }

    /**
     * Returns the last event on the model.
     *
     * @param self
     * @return The model's last event.
     */
    public static ITemplateEvent last(@Nonnull IModel self) {
        return self.get(self.size() - 1);
    }

    /**
     * If the model represents an element open to close tags, then this method
     * removes all of the inner events.
     *
     * @param self
     */
    public static void removeChildren(@Nonnull IModel self) {
        if (isElement(self)) {
            while (self.size() > 2) {
                self.remove(1);
            }
        }
    }

    /**
     * Removes the first event on the model.
     *
     * @param self
     */
    public static void removeFirst(@Nonnull IModel self) {
        self.remove(0);
    }

    /**
     * Removes the last event on the model.
     *
     * @param self
     */
    public static void removeLast(@Nonnull IModel self) {
        self.remove(self.size() - 1);
    }

    /**
     * Removes a models-worth of events from the specified position. What this
     * means is that, if the event at the position is an opening element, then
     * it, and everything up to and including its matching end element, is
     * removed.
     *
     * @param self
     * @param pos A valid index within the current model.
     */
    public static void removeModel(@Nonnull IModel self, int pos) {
        if (0 <= pos && pos < self.size()) {
            int modelSize = sizeOfModelAt(self, pos);
            while (modelSize > 0) {
                self.remove(pos);
                modelSize--;
            }
        }
    }

    /**
     * Replaces the model at the specified index with the given model.
     *
     * @param self
     * @param pos A valid index within the current model.
     * @param model
     */
    public static void replaceModel(@Nonnull IModel self, int pos, @Nullable IModel model) {
        if (0 <= pos && pos < self.size()) {
            removeModel(self, pos);
            // noop if model is null
            // https://github.com/thymeleaf/thymeleaf/blob/thymeleaf-3.0.11.RELEASE/src/main/java/org/thymeleaf/engine/Model.java#L206
            self.insertModel(pos, model);
        }
    }

    /**
     * If an opening element exists at the given position, this method will
     * return the 'size' of that element (number of events from here to its
     * matching closing tag).
     *
     * @param self
     * @param index
     * @return Size of an element from the given position, or 1 if the event at
     * the position isn't an opening element.
     */
    @SuppressWarnings("ValueOfIncrementOrDecrementUsed")
    public static int sizeOfModelAt(@Nonnull IModel self, int index) {
        int eventIndex = index;
        ITemplateEvent event = self.get(eventIndex++);

        if (event instanceof IOpenElementTag) {
            int level = 0;
            while (true) {
                event = self.get(eventIndex++);
                if (event instanceof IOpenElementTag) {
                    level++;
                } else if (event instanceof ICloseElementTag) {
                    //noinspection StatementWithEmptyBody
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
     * @param self
     */
    public static void trim(@Nonnull IModel self) {
        while (ITemplateEventExtensions.isWhitespace(first(self))) {
            removeFirst(self);
        }
        while (ITemplateEventExtensions.isWhitespace(last(self))) {
            removeLast(self);
        }
    }

}
