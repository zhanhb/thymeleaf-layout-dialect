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

import java.util.Iterator;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlElementDecorator;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 *
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator extends XmlElementDecorator {

    private static int finderTitleEventIndex(IModel sourceHeadModel) {
        for (int i = 0; i < sourceHeadModel.size(); i++) {
            ITemplateEvent event = sourceHeadModel.get(i);
            boolean result = event instanceof IOpenElementTag
                    && "title".equals(((IOpenElementTag) event).getElementCompleteName());
            if (result) {
                return i;
            }
        }
        return -1;
    }

    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, sets up the element decorator context.
     *
     * @param modelFactory
     * @param sortingStrategy
     */
    public HtmlHeadDecorator(IModelFactory modelFactory, SortingStrategy sortingStrategy) {
        super(modelFactory);
        this.sortingStrategy = sortingStrategy;
    }

    //@SuppressWarnings("null")
    //private void titleExtraction(Element headElement, String titleType, Element titleContainer, String[] titlePattern) {
    //    Element existingContainer = headElement != null ? MetaClass.findElement(headElement, "title-container") : null;
    //    if (existingContainer != null) {
    //        List<Node> children = existingContainer.getChildren();
    //        Node titleElement = children.isEmpty() ? null : children.get(children.size() - 1);
    //        String attributeValue = MetaClass.getAttributeValue((Element) titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
    //        titlePattern[0] = !StringUtils.isEmpty(attributeValue) ? attributeValue : titlePattern[0];
    //        titleElement.setNodeProperty(TITLE_TYPE, titleType);
    //        MetaClass.removeChildWithWhitespace(headElement, existingContainer);
    //        titleContainer.addChild(existingContainer);
    //    } else {
    //        Element titleElement = headElement != null ? MetaClass.findElement(headElement, "title") : null;
    //        if (titleElement != null) {
    //            String attributeValue = MetaClass.getAttributeValue(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
    //            titlePattern[0] = !StringUtils.isEmpty(attributeValue) ? attributeValue : titlePattern[0];
    //            titleElement.setNodeProperty(TITLE_TYPE, titleType);
    //            MetaClass.removeAttribute(titleElement, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_TITLEPATTERN);
    //            MetaClass.removeChildWithWhitespace(headElement, titleElement);
    //            titleContainer.addChild(titleElement);
    //        }
    //    }
    //}
    /**
     * Decorate the {@code <head>} part.
     *
     * @param targetHeadModel
     * @param targetHeadTemplate
     * @param sourceHeadModel
     * @param sourceHeadTemplate
     */
    @Override
    public void decorate(IModel targetHeadModel, String targetHeadTemplate,
            IModel sourceHeadModel, String sourceHeadTemplate) {

        // Try to ensure there is a head as a result of decoration, applying the
        // source head, or just using what is in the target
        if (!MetaClass.asBoolean(targetHeadModel)) {
            if (MetaClass.asBoolean(sourceHeadModel)) {
                MetaClass.replaceModel(targetHeadModel, sourceHeadModel);
            }
            return;
        }

        // Replace the target title with the source one if present
        IModel sourceTitle;
        int sourceTitleIndex = finderTitleEventIndex(sourceHeadModel);
        if (sourceTitleIndex != -1) {
            sourceTitle = MetaClass.getModel(sourceHeadModel, sourceTitleIndex);
            MetaClass.removeModelWithWhitespace(sourceHeadModel, sourceTitleIndex);

            int targetTitleIndex = finderTitleEventIndex(targetHeadModel);
            if (targetTitleIndex != -1) {
                MetaClass.removeModelWithWhitespace(targetHeadModel, targetTitleIndex);
            }

            MetaClass.insertModelWithWhitespace(targetHeadModel, 1, sourceTitle);
        }

        // TODO: complicated title replacement
/*
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
         */
        // Merge the source <head> elements with the target <head> elements using
        // the current merging strategy, placing the resulting title at the
        // beginning of it
        if (MetaClass.asBoolean(sourceHeadModel)) {
            Iterator<IModel> it = MetaClass.modelIterator(sourceHeadModel);
            while(it.hasNext()){
                IModel sourceHeadSubModel = it.next();
                int position = sortingStrategy.findPositionForModel(targetHeadModel, sourceHeadSubModel);
                if (position != -1) {
                    MetaClass.insertModelWithWhitespace(targetHeadModel, position, sourceHeadSubModel);
                }
            }
        }

        super.decorate(targetHeadModel, targetHeadTemplate, sourceHeadModel, sourceHeadTemplate);
    }

}
