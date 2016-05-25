package nz.net.ultraq.thymeleaf.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import nz.net.ultraq.thymeleaf.models.ModelIterator;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;

/**
 *
 * @author zhanhb
 */
public class MetaClass {

    private static final Map<IModel, Integer> map = new ConcurrentWeakIdentityHashMap<>(200);

    /**
     * Set that a model evaluates to 'false' if it has no events.
     *
     * @param delegate
     * @return {@code true} if this model has events.
     */
    public static boolean asBoolean(IModel delegate) {
        return delegate != null && delegate.size() > 0;
    }

    /**
     * Iterate through each event in the model. This is similar to what the
     * {@code accept} method does.
     *
     * @param delegate
     * @param closure
     */
    public static void each(IModel delegate, Consumer<? super ITemplateEvent> closure) {
        for (int i = 0; i < delegate.size(); i++) {
            closure.accept(delegate.get(i));
        }
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
    public static ITemplateEvent find(IModel delegate, Predicate<? super ITemplateEvent> closure) {
        for (int i = 0; i < delegate.size(); i++) {
            ITemplateEvent event = delegate.get(i);
            boolean result = closure.test(event);
            if (result) {
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
     * @return The first event index to match the closure criteria, or
     * {@code null} if nothing matched.
     */
    public static int findIndexOf(IModel delegate, Predicate<? super ITemplateEvent> closure) {
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
     * Returns the first event on the model.
     *
     * @param delegate
     * @return The model's first event, or {@code null} if the model has no
     * events.
     */
    public static ITemplateEvent first(IModel delegate) {
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
    public static IModel getModel(IModel delegate, int pos) {
        int modelSize = calculateModelSize(delegate, pos);
        IModel subModel = delegate.cloneModel();
        while (pos + modelSize < subModel.size()) {
            removeLast(subModel);
        }
        while (modelSize < subModel.size()) {
            removeFirst(subModel);
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
    public static void insertModelWithWhitespace(IModel delegate, int pos, IModel model) {
        IModel whitespace = getModel(delegate, pos);  // Assumes that whitespace exists at the insertion point
        if (isWhitespace(whitespace)) {
            delegate.insertModel(pos, model);
            delegate.insertModel(pos, whitespace);
        } else {
            delegate.insertModel(pos, model);
        }
    }

    /**
     * Returns whether or not this model represents collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if this is a collapsible text model.
     */
    public static boolean isWhitespace(IModel delegate) {
        return delegate.size() == 1 && isWhitespace(first(delegate));
    }

    /**
     * Returns the last event on the model.
     *
     * @param delegate
     * @return The model's lats event, or {@code null} if the model has no
     * events.
     */
    public static ITemplateEvent last(IModel delegate) {
        return delegate.get(delegate.size() - 1);
    }

    /**
     * Returns a new model iterator over this model.
     *
     * @param delegate
     * @return New model iterator.
     */
    public static Iterator<IModel> modelIterator(IModel delegate) {
        return new ModelIterator(delegate);
    }

    /**
     * Removes the first event on the model.
     *
     * @param delegate
     */
    public static void removeFirst(IModel delegate) {
        delegate.remove(0);
    }

    /**
     * Removes the last event on the model.
     *
     * @param delegate
     */
    public static void removeLast(IModel delegate) {
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
    public static void removeModel(IModel delegate, int pos) {
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
    public static void removeModelWithWhitespace(IModel delegate, int pos) {
        removeModel(delegate, pos);
        ITemplateEvent priorEvent = delegate.get(pos - 1);
        if (isWhitespace(priorEvent)) {
            delegate.remove(pos - 1);
        }
    }

    /**
     * Replaces the enture model with a new one.
     *
     * @param delegate
     * @param model
     */
    public static void replaceModel(IModel delegate, IModel model) {
        delegate.reset();
        delegate.addModel(model);
    }

    /**
     * Returns whether or not this event represents collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if this is a collapsible text node.
     */
    public static boolean isWhitespace(ITemplateEvent delegate) {
        return delegate instanceof IText && isWhitespace((IText) delegate);
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
    public static boolean equalsName(IAttribute delegate, String prefix, String name) {
        String attributeName = delegate.getAttributeCompleteName();
        return (prefix + ":" + name).equals(attributeName) || ("data-" + prefix + "-" + name).equals(attributeName);
    }

    /**
     * Shortcut to the attribute name class on the attribute definition.
     *
     * @param delegate
     * @return Attribute name object.
     */
    public static AttributeName getAttributeName(IAttribute delegate) {
        return delegate.getAttributeDefinition().getAttributeName();
    }

    /**
     * Returns whether or not this text event is collapsible whitespace.
     *
     * @param delegate
     * @return {@code true} if, when trimmed, the text content is empty.
     */
    public static boolean isWhitespace(IText delegate) {
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
    private static int calculateModelSize(IModel model, int index) {
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
                    if (level == 0) {
                        break;
                    }
                    level--;
                }
            }
            return eventIndex - index;
        }

        return 1;
    }

    public static Integer getEndIndex(IModel model) {
        return map.get(model);
    }

    public static void setEndIndex(IModel model, int endIndex) {
        map.put(model, endIndex);
    }

    private MetaClass() {
        throw new AssertionError();
    }

}
