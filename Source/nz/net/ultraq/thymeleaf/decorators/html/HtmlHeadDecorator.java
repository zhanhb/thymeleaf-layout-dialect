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

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import static nz.net.ultraq.thymeleaf.LayoutDialect.DIALECT_PREFIX_LAYOUT;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.PROCESSOR_NAME_TITLEPATTERN;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_CONTENT;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_DECORATOR;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlElementDecorator;
import org.thymeleaf.dom.AbstractTextNode;
import org.thymeleaf.dom.Document;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.NestableNode;
import org.thymeleaf.dom.Node;
import org.thymeleaf.dom.Text;

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 *
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator extends XmlElementDecorator {

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

	/**
	 * Returns an attribute processor's value, checks both prefix:processor and
	 * data-prefix-processor variants.
	 *
	 * @param prefix
	 * @param name
	 * @return The value of the matching processor, or <tt>null</tt> if the
	 * processor doesn't exist on the element.
	 */
	private static String getAttributeValue(Element delegate, String prefix, String name) {
		String attributeValue = delegate.getAttributeValue(prefix + ":" + name);
		if (attributeValue == null || attributeValue.isEmpty()) {
			attributeValue = delegate.getAttributeValue("data-" + prefix + "-" + name);
		}
		return attributeValue;
	}

	/**
	 * Removes an attribute processor from this element, checks both
	 * prefix:processor and data-prefix-processor variants.
	 *
	 * @param prefix
	 * @param name
	 */
	private static void removeAttribute(Element delegate, String prefix, String name) {
		delegate.removeAttribute(prefix + ":" + name);
		delegate.removeAttribute("data-" + prefix + "-" + name);
	}

	/**
	 * Inserts a child node, creating a whitespace node before it so that it
	 * appears in line with all the existing children.
	 *
	 * @param child Node to add.
	 * @param index Node position.
	 */
	private static void insertChildWithWhitespace(Element delegate, Node child, int index) {
		if (child != null) {
			NestableNode parent = delegate.getParent();
			Text whitespace;
			if (parent instanceof Document) {
				whitespace = new Text("\n\t");
			} else {
				List<Node> parentChildren = parent.getChildren();
				Node get = parentChildren.get(parentChildren.indexOf(delegate) - 1);
				whitespace = new Text(((AbstractTextNode) get).getContent() + '\t');
			}
			delegate.insertChild(index, whitespace);
			delegate.insertChild(index + 1, child);
		}
	}

	/**
	 * Removes a child node and the whitespace node immediately before so that
	 * the area doesn't appear too messy.
	 *
	 * @param child Node to remove
	 */
	private static void removeChildWithWhitespace(Element delegate, Node child) {
		if (child != null) {
			List<Node> children = delegate.getChildren();
			int index = children.indexOf(child);
			delegate.removeChild(index);
			if (index > 0) {
				delegate.removeChild(index - 1);
			}
		}
	}

	final SortingStrategy sortingStrategy;

	public HtmlHeadDecorator(SortingStrategy sortingStrategy) {
		this.sortingStrategy = sortingStrategy;
	}

	/**
	 * Decorate the {@code <head>} part, appending all of the content
	 * {@code <head>} elements on to the decorator {@code <head>} elements.
	 *
	 * @param decoratorHtml Decorator's {@code <head>} element.
	 * @param contentHead	Content's {@code <head>} element.
	 */
	@Override
	public void decorate(Element decoratorHtml, Element contentHead) {

		// If the decorator has no <head>, then we can just use the content <head>
		Element decoratorHead = findElement(decoratorHtml, "head");
		if (decoratorHead == null) {
			if (contentHead != null) {
				insertChildWithWhitespace(decoratorHtml, contentHead, 0);
				Element contentTitle = findElement(contentHead, "title");
				if (contentTitle != null) {
					removeAttribute(contentTitle, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
				}
			}
			return;
		}

		// Copy the content and decorator <title>s
		// TODO: Surely the code below can be simplified?  The 2 conditional
		//       blocks are doing almost the same thing.
		Element titleContainer = new Element("title-container");
		String[] titlePattern = new String[1];
		BiConsumer<Element, String> titleExtraction = (headElement, titleType) -> {
			Element existingContainer = headElement != null ? findElement(headElement, "title-container") : null;
			if (existingContainer != null) {
				List<Node> children = existingContainer.getChildren();
				Node titleElement = children.isEmpty() ? null : children.get(children.size() - 1);
				String attributeValue = getAttributeValue((Element) titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
				titlePattern[0] = attributeValue != null && !attributeValue.isEmpty() ? attributeValue : titlePattern[0];
				titleElement.setNodeProperty(TITLE_TYPE, titleType);
				removeChildWithWhitespace(headElement, existingContainer);
				titleContainer.addChild(existingContainer);
			} else {
				Element titleElement = headElement != null ? findElement(headElement, "title") : null;
				if (titleElement != null) {
					String attributeValue = getAttributeValue(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
					titlePattern[0] = attributeValue != null && !attributeValue.isEmpty() ? attributeValue : titlePattern[0];
					titleElement.setNodeProperty(TITLE_TYPE, titleType);
					removeAttribute(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
					removeChildWithWhitespace(headElement, titleElement);
					titleContainer.addChild(titleElement);
				}
			}
		};
		titleExtraction.accept(decoratorHead, TITLE_TYPE_DECORATOR);
		titleExtraction.accept(contentHead, TITLE_TYPE_CONTENT);

		Element resultTitle = new Element("title");
		resultTitle.setAttribute(DIALECT_PREFIX_LAYOUT + ":" + PROCESSOR_NAME_TITLEPATTERN, titlePattern[0]);
		titleContainer.addChild(resultTitle);

		// Merge the content's <head> elements with the decorator's <head>
		// section via the given merging strategy, placing the resulting title
		// at the beginning of it
		if (contentHead != null) {
			for (Node contentHeadChild : contentHead.getChildren()) {
				List<Node> decoratorHeadChildren = decoratorHead.getChildren();
				int position = sortingStrategy.findPositionForContent(decoratorHeadChildren, contentHeadChild);
				if (position != -1) {
					insertChildWithWhitespace(decoratorHead, contentHeadChild, position);
				}
			}
		}
		insertChildWithWhitespace(decoratorHead, titleContainer, 0);

		super.decorate(decoratorHead, contentHead);
	}
}
