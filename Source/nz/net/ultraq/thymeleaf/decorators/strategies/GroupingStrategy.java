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
import java.util.stream.Collectors;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import org.thymeleaf.dom.AbstractTextNode;
import org.thymeleaf.dom.Node;
import org.thymeleaf.dom.Text;

/**
 * The {@code <head>} merging strategy which groups like elements together.
 *
 * @author Emanuel Rabina
 * @since 1.2.6
 */
public class GroupingStrategy implements SortingStrategy {

	/**
	 * Returns whether or not this node represents collapsible whitespace.
	 *
	 * @return <tt>true</tt> if this is a collapsible text node.
	 */
	private static boolean isWhitespaceNode(Node delegate) {
		return delegate instanceof Text && ((AbstractTextNode) delegate).getContent().trim().isEmpty();
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
		if (isWhitespaceNode(contentNode)) {
			return -1;
		}

		HeadNodeTypes type = HeadNodeTypes.findMatchingType(contentNode);
		List<Boolean> collect = decoratorNodes.stream().map(decoratorNode
				-> type == HeadNodeTypes.findMatchingType(decoratorNode)
		).collect(Collectors.toList());
		return collect.lastIndexOf(Boolean.TRUE) + 1;
	}
}
