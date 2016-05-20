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
package nz.net.ultraq.thymeleaf.decorators.html;

import java.util.Objects;
import java.util.function.Function;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlDocumentDecorator;
import org.thymeleaf.dom.Document;
import org.thymeleaf.dom.Element;

/**
 * A decorator made to work over whole HTML pages. Decoration will be done in 2
 * phases: a special one for the HEAD element, and a generic one for the BODY
 * element.
 *
 * @author Emanuel Rabina
 */
public class HtmlDocumentDecorator extends XmlDocumentDecorator {

	/**
	 * Searches this and all children of this element for an element of the
	 * given name.
	 *
	 * @param name
	 * @return The matching element, or <tt>null</tt> if no match was found.
	 */
	private static Element findElement(Element delegate, String name) {
		Function<Element, Element>[] search = new Function[1];
		search[0] = element -> {
			if (Objects.equals(element.getOriginalName(), name)) {
				return element;
			}
			return element.getElementChildren().stream().map(search[0]).filter(Objects::nonNull)
					.findFirst().orElse(null);
		};
		return search[0].apply(delegate);
	}

	final SortingStrategy sortingStrategy;

	public HtmlDocumentDecorator(SortingStrategy sortingStrategy) {
		this.sortingStrategy = sortingStrategy;
	}

	/**
	 * Decorate an entire HTML page.
	 *
	 * @param decoratorHtml Decorator's HTML element.
	 * @param contentHtml	Content's HTML element.
	 */
	@Override
	public void decorate(Element decoratorHtml, Element contentHtml) {

		new HtmlHeadDecorator(sortingStrategy).decorate(decoratorHtml, findElement(contentHtml, "head"));
		new HtmlBodyDecorator().decorate(decoratorHtml, findElement(contentHtml, "body"));

		// Set the doctype from the decorator if missing from the content page
		Document decoratorDocument = (Document) decoratorHtml.getParent();
		Document contentDocument = (Document) contentHtml.getParent();
		if (contentDocument.getDocType() == null && decoratorDocument.getDocType() != null) {
			contentDocument.setDocType(decoratorDocument.getDocType());
		}

		super.decorate(decoratorHtml, contentHtml);
	}
}
