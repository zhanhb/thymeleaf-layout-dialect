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
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.MetaProvider;
import nz.net.ultraq.thymeleaf.models.AttributeMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;

/**
 * A decorator specific to processing an HTML {@code <head>} element.
 *
 * @author Emanuel Rabina
 */
public class HtmlHeadDecorator implements Decorator {

    // Get the source and target title elements
    private static IModel titleRetriever(IModel headModel) {
        IModel title = MetaClass.findModel(headModel, event -> {
            return event instanceof IOpenElementTag && "title".equals(((IElementTag) event).getElementCompleteName());
        });
        if (MetaClass.asBoolean(title)) {
            MetaClass.removeModelWithWhitespace(headModel, MetaProvider.INSTANCE.getProperty(title, "startIndex"));
        }
        return title;
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
        // If one of the parameters is missing return a copy of the other, or
        // nothing if both parameters are missing.
        if (!MetaClass.asBoolean(targetHeadModel) || !MetaClass.asBoolean(sourceHeadModel)) {
            return MetaClass.asBoolean(targetHeadModel) ? targetHeadModel.cloneModel() : MetaClass.asBoolean(sourceHeadModel) ? sourceHeadModel.cloneModel() : null;
        }

        IModel resultTitle = new HtmlTitleDecorator(context).decorate(
                titleRetriever(targetHeadModel),
                titleRetriever(sourceHeadModel));
        MetaClass.insertModelWithWhitespace(targetHeadModel, 1, resultTitle);

        // Merge the source <head> elements with the target <head> elements using
        // the current merging strategy, placing the resulting title at the
        // beginning of it
        if (MetaClass.asBoolean(sourceHeadModel)) {
            Iterator<IModel> it = MetaClass.childModelIterator(sourceHeadModel);
            if (it != null) {
                while (it.hasNext()) {
                    IModel childModel = it.next();
                    int position = sortingStrategy.findPositionForModel(targetHeadModel, childModel);
                    if (position != -1) {
                        MetaClass.insertModelWithWhitespace(targetHeadModel, position, childModel);
                    }
                }
            }
        }
        return new AttributeMerger(context.getModelFactory()).merge(targetHeadModel, sourceHeadModel);
    }

}
