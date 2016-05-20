/*
 * Copyright 2013, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.decorators.xml;

import java.util.Objects;
import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.fragments.mergers.ElementMerger;
import org.thymeleaf.dom.Comment;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.NestableNode;
import org.thymeleaf.dom.Node;

/**
 * A decorator made to work over any Thymeleaf document.
 *
 * @author Emanuel Rabina
 */
public class XmlDocumentDecorator implements Decorator {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void decorate(Element decoratorXml, Element contentXml) {

		NestableNode decoratorDocument = decoratorXml.getParent();
		NestableNode contentDocument = contentXml.getParent();

		// Copy text outside of the root element, keeping whitespace copied to a minimum
		boolean beforeHtml = true;
		boolean allowNext = false;
		Node lastNode = contentXml;
		for (Node externalNode : decoratorDocument.getChildren()) {
			if (externalNode == decoratorXml) {
				beforeHtml = false;
				allowNext = true;
				continue;
			}
			if (externalNode instanceof Comment || allowNext) {
				if (beforeHtml) {
					contentDocument.insertBefore(contentXml, externalNode);
				} else {
					contentDocument.insertAfter(lastNode, externalNode);
					lastNode = externalNode;
				}
				allowNext = externalNode instanceof Comment;
			}
		}

		// Bring the decorator into the content page (which is the one being processed)
		new ElementMerger(!Objects.equals(decoratorXml.getNormalizedName(), contentXml.getNormalizedName())).merge(contentXml, decoratorXml);
	}
}
