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
import nz.net.ultraq.thymeleaf.internal.MetaProvider;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;

/**
 * A decorator made to work over an HTML document. Decoration for a document
 * involves 2 sub-decorators: a special one for the {@code <head>} element, and
 * a standard one for the {@code <body>} element.
 *
 * @author Emanuel Rabina
 */
@lombok.experimental.ExtensionMethod(nz.net.ultraq.thymeleaf.internal.MetaClass.class)
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
        IModel targetHeadModel = targetDocumentModel.findModel(headModelFinder);
        IModel resultHeadModel = new HtmlHeadDecorator(context, sortingStrategy).decorate(targetHeadModel,
                sourceDocumentModel.findModel(headModelFinder)
        );
        if (resultHeadModel.asBoolean()) {
            if (targetHeadModel.asBoolean()) {
                targetDocumentModel.replaceModel(MetaProvider.INSTANCE.getProperty(targetHeadModel, "startIndex"), resultHeadModel);
            } else {
                targetDocumentModel.insertModelWithWhitespace(targetDocumentModel.findIndexOf(event -> {
                    return (event instanceof IOpenElementTag && "body".equals(((IElementTag) event).getElementCompleteName()))
                            || (event instanceof ICloseElementTag && "html".equals(((IElementTag) event).getElementCompleteName()));
                }) - 1, resultHeadModel);
            }
        }

        // Body decoration
        ITemplateEventPredicate bodyModelFinder = event -> {
            return event instanceof IOpenElementTag && "body".equals(((IElementTag) event).getElementCompleteName());
        };
        IModel targetBodyModel = targetDocumentModel.findModel(bodyModelFinder);
        IModel resultBodyModel = new HtmlBodyDecorator(context.getModelFactory()).decorate(targetBodyModel,
                sourceDocumentModel.findModel(bodyModelFinder)
        );
        if (resultBodyModel.asBoolean()) {
            if (targetBodyModel.asBoolean()) {
                targetDocumentModel.replaceModel(MetaProvider.INSTANCE.getProperty(targetBodyModel, "startIndex"), resultBodyModel);
            } else {
                targetDocumentModel.insertModelWithWhitespace(targetDocumentModel.findIndexOf(event -> {
                    return event instanceof ICloseElementTag && "html".equals(((IElementTag) event).getElementCompleteName());
                }) - 1, resultBodyModel);
            }
        }

        return super.decorate(targetDocumentModel, sourceDocumentModel);
    }

}
