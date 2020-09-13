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
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import nz.net.ultraq.thymeleaf.models.extensions.ITemplateEventExtensions;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;

/**
 * A decorator made to work over an HTML document. Decoration for a document
 * involves 2 sub-decorators: a special one for the {@code <head>} element, and
 * a standard one for the {@code <body>} element.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class HtmlDocumentDecorator extends XmlDocumentDecorator {

    private final boolean autoHeadMerging;
    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, apply the given sorting strategy to the decorator.
     *
     * @param context
     * @param sortingStrategy
     * @param autoHeadMerging
     */
    public HtmlDocumentDecorator(ITemplateContext context, SortingStrategy sortingStrategy, boolean autoHeadMerging) {
        super(context);
        this.sortingStrategy = sortingStrategy;
        this.autoHeadMerging = autoHeadMerging;
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
        IModelFactory modelFactory = context.getModelFactory();
        IModel resultDocumentModel = targetDocumentModel.cloneModel();
        // Head decoration
        ITemplateEventPredicate headModelFinder = event -> ITemplateEventExtensions.isOpeningElementOf(event, "head");

        if (autoHeadMerging) {
            IModel targetHeadModel = IModelExtensions.findModel(resultDocumentModel, headModelFinder);
            IModel resultHeadModel = new HtmlHeadDecorator(context, sortingStrategy).decorate(targetHeadModel,
                    IModelExtensions.findModel(sourceDocumentModel, headModelFinder)
            );
            if (IModelExtensions.asBoolean(resultHeadModel)) {
                if (IModelExtensions.asBoolean(targetHeadModel)) {
                    IModelExtensions.replaceModel(resultDocumentModel, IModelExtensions.findIndexOfModel(resultDocumentModel, targetHeadModel), resultHeadModel);
                } else {
                    IModelExtensions.insertModelWithWhitespace(resultDocumentModel, IModelExtensions.findIndexOf(resultDocumentModel, event -> {
                        return ITemplateEventExtensions.isOpeningElementOf(event, "body")
                                || ITemplateEventExtensions.isClosingElementOf(event, "html");
                    }) - 1, resultHeadModel, modelFactory);
                }
            }
        } else {
            // TODO: If autoHeadMerging is false, this really shouldn't be needed as
            //       the basis for `resultDocumentModel` should be the source model.
            //       This 'hack' is OK for an experimental option, but the fact that
            //       it exists means I should rethink how the result model is made.
            IModelExtensions.replaceModel(resultDocumentModel,
                    IModelExtensions.findIndexOf(resultDocumentModel, headModelFinder),
                    IModelExtensions.findModel(sourceDocumentModel, headModelFinder)
            );
        }

        // Body decoration
        ITemplateEventPredicate bodyModelFinder = event -> ITemplateEventExtensions.isOpeningElementOf(event, "body");
        IModel targetBodyModel = IModelExtensions.findModel(resultDocumentModel, bodyModelFinder);
        IModel resultBodyModel = new HtmlBodyDecorator(context).decorate(targetBodyModel,
                IModelExtensions.findModel(sourceDocumentModel, bodyModelFinder)
        );
        if (IModelExtensions.asBoolean(resultBodyModel)) {
            if (IModelExtensions.asBoolean(targetBodyModel)) {
                IModelExtensions.replaceModel(resultDocumentModel, IModelExtensions.findIndexOfModel(resultDocumentModel, targetBodyModel), resultBodyModel);
            } else {
                IModelExtensions.insertModelWithWhitespace(resultDocumentModel, IModelExtensions.findIndexOf(resultDocumentModel, event -> {
                    return ITemplateEventExtensions.isClosingElementOf(event, "html");
                }) - 1, resultBodyModel, modelFactory);
            }
        }

        return super.decorate(resultDocumentModel, sourceDocumentModel);
    }

    public final SortingStrategy getSortingStrategy() {
        return sortingStrategy;
    }

    public final boolean getAutoHeadMerging() {
        return autoHeadMerging;
    }

    public final boolean isAutoHeadMerging() {
        return autoHeadMerging;
    }

}
