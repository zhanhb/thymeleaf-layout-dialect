/*
 * Copyright 2015, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.decorators.strategies;

import java.util.ArrayList;
import java.util.ListIterator;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.models.extensions.ChildModelIterator;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import nz.net.ultraq.thymeleaf.models.extensions.ITemplateEventExtensions;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;

/**
 * The {@code <head>} merging strategy which groups like elements together.
 * <p>
 * The default behaviour of the layout dialect has historically been to place
 * the {@code <title>} element at the beginning of the {@code <head>} element
 * during the decoration process; an arbitrary design decision which made
 * development of this library easier. However, this runs against the
 * expectations of developers who wished to control the order of elements, most
 * notably the position of a {@code <meta charset...>} element.
 * <p>
 * This sorting strategy has been updated in 2.4.0 to retain this behaviour as
 * backwards compatibility with the 2.x versions of the layout dialect, but is
 * now deprecated and expected to be replaced by the
 * {@link GroupingRespectLayoutTitleStrategy} sorter from version 3.x onwards.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 * @since 1.2.6
 */
@Deprecated
public class GroupingStrategy implements SortingStrategy {

    /**
     * Figure out the enum for the given model.
     *
     * @param model
     * @return Matching enum to describe the model.
     */
    private static int findMatchingType(IModel model) {
        final int COMMENT = 1;
        final int META = 2;
        final int SCRIPT = 3;
        final int STYLE = 4;
        final int STYLESHEET = 5;
        final int OTHER = 6;

        ITemplateEvent event = IModelExtensions.first(model);

        if (event instanceof IComment) {
            return COMMENT;
        }
        if (event instanceof IElementTag) {
            String elementCompleteName = ((IElementTag) event).getElementCompleteName();
            if (event instanceof IProcessableElementTag && "meta".equals(elementCompleteName)) {
                return META;
            }
            if (event instanceof IOpenElementTag && "script".equals(elementCompleteName)) {
                return SCRIPT;
            }
            if (event instanceof IOpenElementTag && "style".equals(elementCompleteName)) {
                return STYLE;
            }
            if (event instanceof IProcessableElementTag && "link".equals(elementCompleteName)
                    && "stylesheet".equals(((IProcessableElementTag) event).getAttributeValue("rel"))) {
                return STYLESHEET;
            }
            return OTHER;
        }
        return 0;
    }

    /**
     * Returns the index of the last set of elements that are of the same 'type'
     * as the content node. eg: groups scripts with scripts, stylesheets with
     * stylesheets, and so on.
     *
     * @param headModel
     * @param childModel
     * @return Position of the end of the matching element group.
     */
    @Override
    public int findPositionForModel(IModel headModel, IModel childModel) {
        // Discard text/whitespace nodes
        if (IModelExtensions.isWhitespace(childModel)) {
            return -1;
        }

        // For backwards compatibility, match the location of any element at the
        // beginning of the <head> element.
        if (IModelExtensions.isElementOf(childModel, "title")) {
            int firstElementIndex = IModelExtensions.findIndexOf(headModel, 1, ITemplateEventExtensions::isOpeningElement);
            if (firstElementIndex != -1) {
                return firstElementIndex;
            }
            return headModel.size() > 2 ? 2 : 1;
        }

        int type = findMatchingType(childModel);
        ArrayList<IModel> list = new ArrayList<>(20);

        ChildModelIterator it = IModelExtensions.childModelIterator(headModel);
        if (it != null) {
            while (it.hasNext()) {
                list.add(it.next());
            }
        }

        ListIterator<IModel> listIterator = list.listIterator(list.size());
        while (listIterator.hasPrevious()) {
            IModel headSubModel = listIterator.previous();
            if (type == findMatchingType(headSubModel)) {
                if (IModelExtensions.asBoolean(headModel)) {
                    return IModelExtensions.findIndexOfModel(headModel, headSubModel) + headSubModel.size();
                }
                break;
            }
        }

        return 1;
    }

}
