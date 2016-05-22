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
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlElementDecorator;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;
import org.thymeleaf.util.StringUtils;

import static nz.net.ultraq.thymeleaf.LayoutDialect.DIALECT_PREFIX_LAYOUT;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.PROCESSOR_NAME_TITLEPATTERN;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_CONTENT;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.TITLE_TYPE_DECORATOR;

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 *
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator extends XmlElementDecorator {

    private final SortingStrategy sortingStrategy;

    public HtmlHeadDecorator(SortingStrategy sortingStrategy) {
        this.sortingStrategy = sortingStrategy;
    }

    @SuppressWarnings("null")
    private void titleExtraction(Element headElement, String titleType, Element titleContainer, String[] titlePattern) {
        Element existingContainer = headElement != null ? MetaClass.findElement(headElement, "title-container") : null;
        if (existingContainer != null) {
            List<Node> children = existingContainer.getChildren();
            Node titleElement = children.isEmpty() ? null : children.get(children.size() - 1);
            String attributeValue = MetaClass.getAttributeValue((Element) titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
            titlePattern[0] = !StringUtils.isEmpty(attributeValue) ? attributeValue : titlePattern[0];
            titleElement.setNodeProperty(TITLE_TYPE, titleType);
            MetaClass.removeChildWithWhitespace(headElement, existingContainer);
            titleContainer.addChild(existingContainer);
        } else {
            Element titleElement = headElement != null ? MetaClass.findElement(headElement, "title") : null;
            if (titleElement != null) {
                String attributeValue = MetaClass.getAttributeValue(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
                titlePattern[0] = !StringUtils.isEmpty(attributeValue) ? attributeValue : titlePattern[0];
                titleElement.setNodeProperty(TITLE_TYPE, titleType);
                MetaClass.removeAttribute(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
                MetaClass.removeChildWithWhitespace(headElement, titleElement);
                titleContainer.addChild(titleElement);
            }
        }
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
        Element decoratorHead = MetaClass.findElement(decoratorHtml, "head");
        if (decoratorHead == null) {
            if (contentHead != null) {
                MetaClass.insertChildWithWhitespace(decoratorHtml, contentHead, 0);
                Element contentTitle = MetaClass.findElement(contentHead, "title");
                if (contentTitle != null) {
                    MetaClass.removeAttribute(contentTitle, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
                }
            }
            return;
        }

        // Copy the content and decorator <title>s
        // TODO: Surely the code below can be simplified?  The 2 conditional
        //       blocks are doing almost the same thing.
        Element titleContainer = new Element("title-container");
        String[] titlePattern = {null};

        titleExtraction(decoratorHead, TITLE_TYPE_DECORATOR, titleContainer, titlePattern);
        titleExtraction(contentHead, TITLE_TYPE_CONTENT, titleContainer, titlePattern);

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
                    MetaClass.insertChildWithWhitespace(decoratorHead, contentHeadChild, position);
                }
            }
        }
        MetaClass.insertChildWithWhitespace(decoratorHead, titleContainer, 0);

        super.decorate(decoratorHead, contentHead);
    }
}
