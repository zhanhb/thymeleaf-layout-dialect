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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
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

        HeadNodeTypes type = HeadNodeTypes.findMatchingType(contentNode);
        return MetaClass.lastIndexOf(decoratorNodes, decoratorNode
                -> type == HeadNodeTypes.findMatchingType(decoratorNode)) + 1;
    }

    /**
     * Enum for the types of elements in the HEAD section that we might need to
     * sort.
     *
     * TODO: Expand this to include more element types as they are requested.
     */
    private static enum HeadNodeTypes {

        COMMENT(node -> node instanceof Comment),
        META(node -> node instanceof Element && "meta".equals(((Element) node).getNormalizedName())),
        STYLESHEET(node -> node instanceof Element && "link".equals(((Element) node).getNormalizedName()) && "stylesheet".equals(((Element) node).getAttributeValue("rel"))),
        SCRIPT(node -> node instanceof Element && "script".equals(((Element) node).getNormalizedName())),
        OTHER_ELEMENT(node -> node instanceof Element);

        final Predicate<Node> determinant;

        /**
         * Constructor, set the test that matches this type of head node.
         *
         * @param determinant
         */
        HeadNodeTypes(Predicate<Node> determinant) {
            this.determinant = determinant;
        }

        /**
         * Figure out the enum for the given node type.
         *
         * @param element The node to match.
         * @return Matching <tt>HeadNodeTypes</tt> enum to descript the node.
         */
        static HeadNodeTypes findMatchingType(Node element) {
            return Arrays.stream(values()).filter(headNodeType
                    -> headNodeType.determinant.test(element)
            ).findFirst().orElse(null);
        }
    }

}
