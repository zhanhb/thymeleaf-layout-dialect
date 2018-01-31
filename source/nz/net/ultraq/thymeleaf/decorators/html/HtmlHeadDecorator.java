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
import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.internal.Extensions;
import nz.net.ultraq.thymeleaf.internal.ITemplateEventPredicate;
import nz.net.ultraq.thymeleaf.models.AttributeMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator implements Decorator {

    private static IModel titleRetriever(IModel headModel, ITemplateEventPredicate isTitle) {
        return Extensions.asBoolean(headModel) ? Extensions.findModel(headModel, isTitle) : null;
    }

    private final ITemplateContext context;
    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, sets up the decorator context.
     *
     * @param context
     * @param sortingStrategy
     */
    public HtmlHeadDecorator(ITemplateContext context, SortingStrategy sortingStrategy) {
        this.context = context;
        this.sortingStrategy = sortingStrategy;
    }

    /**
     * Decorate the {@code <head>} part.
     *
     * @param targetHeadModel
     * @param sourceHeadModel
     * @return Result of the decoration.
     */
    @Override
    public IModel decorate(IModel targetHeadModel, IModel sourceHeadModel) {
        // If none of the parameters are present, return nothing
        if (!Extensions.asBoolean(targetHeadModel) && !Extensions.asBoolean(sourceHeadModel)) {
            return null;
        }

        IModelFactory modelFactory = context.getModelFactory();
        ITemplateEventPredicate isTitle = event -> event instanceof IOpenElementTag && "title".equals(((IElementTag) event).getElementCompleteName());

        // New head model based off the target being decorated
        IModel resultHeadModel = new AttributeMerger(context).merge(targetHeadModel, sourceHeadModel);
        int titleIndex = Extensions.findIndexOf(resultHeadModel, isTitle);
        if (titleIndex != -1) {
            Extensions.removeModelWithWhitespace(resultHeadModel, titleIndex);
        }

        // Get the source and target title elements to pass to the title decorator
        IModel resultTitle = new HtmlTitleDecorator(context).decorate(titleRetriever(targetHeadModel, isTitle),
                titleRetriever(sourceHeadModel, isTitle)
        );
        Extensions.insertModelWithWhitespace(resultHeadModel, 1, resultTitle, modelFactory);

        // Merge the rest of the source <head> elements with the target <head>
        // elements using the current merging strategy
        if (Extensions.asBoolean(sourceHeadModel) && Extensions.asBoolean(targetHeadModel)) {
            for (Iterator<IModel> it = Extensions.childModelIterator(sourceHeadModel); it.hasNext();) {
                IModel model = it.next();
                if (isTitle.test(Extensions.first(model))) {
                    continue;
                }
                int position = sortingStrategy.findPositionForModel(resultHeadModel, model);
                if (position != -1) {
                    Extensions.insertModelWithWhitespace(resultHeadModel, position, model, modelFactory);
                }
            }
        }
        return resultHeadModel;
    }

}
