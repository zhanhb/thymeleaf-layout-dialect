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

import java.util.List;
import java.util.ListIterator;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import org.thymeleaf.dom.Comment;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;

/**
 * The {@code <head>} merging strategy which groups like elements together.
 *
 * @author Emanuel Rabina
 * @since 1.2.6
 */
public class GroupingStrategy implements SortingStrategy {

    private static final int COMMENT = 1;
    private static final int META = 2;
    private static final int STYLESHEET = 3;
    private static final int SCRIPT = 4;
    private static final int OTHER_ELEMENT = 5;

    /**
     * Figure out the type for the given node type.
     *
     * @param node The node to match.
     * @return Matching <tt>int</tt> type to descript the node.
     */
    private static int findMatchingType(Node node) {
        if (node instanceof Comment) {
            return COMMENT;
        } else if (node instanceof Element) {
            Element element = (Element) node;
            String normalizedName = element.getNormalizedName();
            if (normalizedName != null) {
                switch (normalizedName) {
                    case "meta":
                        return META;
                    case "script":
                        return SCRIPT;
                    case "link":
                        if ("stylesheet".equals(element.getAttributeValue("rel"))) {
                            return STYLESHEET;
                        }
                        break;
                }
            }
            return OTHER_ELEMENT;
        }
        return 0;
    }

    /**
     * Returns the index of the last set of elements that are of the same 'type'
     * as the content node. eg: groups scripts with scripts, stylesheets with
     * stylesheets, and so on.
     *
     * @param decoratorNodes
     * @param contentNode
     * @return Position of the end of the matching element group.
     */
    @Override
    public int findPositionForContent(List<Node> decoratorNodes, Node contentNode) {
        // Discard text/whitespace nodes
        if (MetaClass.isWhitespaceNode(contentNode)) {
            return -1;
        }

        int type = findMatchingType(contentNode);
        ListIterator<Node> it = decoratorNodes.listIterator(decoratorNodes.size());

        while (it.hasPrevious()) {
            if (type == findMatchingType(it.previous())) {
                return it.nextIndex() + 1;
            }
        }
        return 0;
    }

}
