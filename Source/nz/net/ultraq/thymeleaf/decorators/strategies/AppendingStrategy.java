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
import org.thymeleaf.dom.Node;

/**
 * The standard {@code <head>} merging strategy, which simply appends the
 * content to the decorator.
 *
 * @author Emanuel Rabina
 */
public class AppendingStrategy implements SortingStrategy {

    /**
     * Returns a value to append the content node to the end of the decorator
     * nodes.
     *
     * @param decoratorNodes
     * @param contentNode
     * @return The size of the decorator nodes list.
     */
    @Override
    public int findPositionForContent(List<Node> decoratorNodes, Node contentNode) {
        if (MetaClass.isWhitespaceNode(contentNode)) {
            return -1;
        }
        ListIterator<Node> it = decoratorNodes.listIterator(decoratorNodes.size());

        while (it.hasPrevious()) {
            if (!MetaClass.isWhitespaceNode(it.previous())) {
                return it.nextIndex() + 1;
            }
        }
        return 0;
    }

}
