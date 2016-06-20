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

import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlDocumentDecorator;
import nz.net.ultraq.thymeleaf.internal.ITemplateEventPredicate;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.MetaProvider;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;

/**
 * A decorator made to work over an HTML document. Decoration for a document
 * involves 2 sub-decorators: a special one of the {@code <head>} element, and a
 * standard one for the {@code <body>} element.
 *
 * @author Emanuel Rabina
 */
public class HtmlDocumentDecorator extends XmlDocumentDecorator {

    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, apply the given sorting strategy to the decorator.
     *
     * @param context
     * @param sortingStrategy
     */
    public HtmlDocumentDecorator(ITemplateContext context, SortingStrategy sortingStrategy) {
        super(context);
        this.sortingStrategy = sortingStrategy;
    }

    /**
     * Decorate an entire HTML page.
     *
     * @param targetDocumentModel
     * @param sourceDocumentModel
     * @return Result of the decoration.
     */
    @Override
    public IModel decorate(IModel targetDocumentModel, IModel sourceDocumentModel) {
        // Head decoration
        ITemplateEventPredicate headModelFinder = event -> {
            return event instanceof IOpenElementTag && "head".equals(((IElementTag) event).getElementCompleteName());
        };
        IModel targetHeadModel = MetaClass.findModel(targetDocumentModel, headModelFinder);
        IModel resultHeadModel = new HtmlHeadDecorator(context, sortingStrategy).decorate(
                targetHeadModel,
                MetaClass.findModel(sourceDocumentModel, headModelFinder)
        );
        if (MetaClass.asBoolean(resultHeadModel)) {
            if (MetaClass.asBoolean(targetHeadModel)) {
                MetaClass.replaceModel(targetDocumentModel, MetaProvider.INSTANCE.getProperty(targetHeadModel, "startIndex"), resultHeadModel);
            } else {
                MetaClass.insertModelWithWhitespace(targetDocumentModel, (Integer) MetaProvider.INSTANCE.getProperty(MetaClass.find(targetDocumentModel, event -> {
                    return (event instanceof IOpenElementTag && "body".equals(((IElementTag) event).getElementCompleteName()))
                            || (event instanceof ICloseElementTag && "html".equals(((IElementTag) event).getElementCompleteName()));
                }), "index") - 1, resultHeadModel);
            }
        }

        // Body decoration
        ITemplateEventPredicate bodyModelFinder = event -> {
            return event instanceof IOpenElementTag && "body".equals(((IElementTag) event).getElementCompleteName());
        };
        IModel targetBodyModel = MetaClass.findModel(targetDocumentModel, bodyModelFinder);
        IModel resultBodyModel = new HtmlBodyDecorator(context.getModelFactory()).decorate(
                targetBodyModel,
                MetaClass.findModel(sourceDocumentModel, bodyModelFinder)
        );
        if (MetaClass.asBoolean(resultBodyModel)) {
            if (MetaClass.asBoolean(targetBodyModel)) {
                MetaClass.replaceModel(targetDocumentModel, MetaProvider.INSTANCE.getProperty(targetBodyModel, "startIndex"), resultBodyModel);
            } else {
                MetaClass.insertModelWithWhitespace(targetDocumentModel, (Integer) MetaProvider.INSTANCE.getProperty(MetaClass.find(targetDocumentModel, event -> {
                    return event instanceof ICloseElementTag && "html".equals(((IElementTag) event).getElementCompleteName());
                }), "index") - 1, resultBodyModel);
            }
        }

        // TODO
        // Set the doctype from the decorator if missing from the content page
//		def decoratorDocument = decoratorModel.parent
//		def contentDocument   = contentModel.parent
//		if (!contentDocument.docType && decoratorDocument.docType) {
//			contentDocument.docType = decoratorDocument.docType
//		}
        return super.decorate(targetDocumentModel, sourceDocumentModel);
    }

}
