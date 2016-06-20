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
import java.util.LinkedHashMap;
import java.util.Map;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.MetaProvider;
import nz.net.ultraq.thymeleaf.internal.ModelBuilder;
import nz.net.ultraq.thymeleaf.models.AttributeMerger;
import nz.net.ultraq.thymeleaf.models.ElementMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardTextTagProcessor;
import org.thymeleaf.util.StringUtils;

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

    private static IAttribute titlePatternProcessorRetriever(IModel titleModel) {
        if (!MetaClass.asBoolean(titleModel)) {
            return null;
        }
        ITemplateEvent event = MetaClass.first(titleModel);
        return event != null ? ((IProcessableElementTag) event).getAttribute(LayoutDialect.DIALECT_PREFIX, TitlePatternProcessor.PROCESSOR_NAME) : null;
    }

    private static String titleValueRetriever(IModel titleModel) {
        ITemplateEvent first = MetaClass.first(titleModel);
        String layout = ((IProcessableElementTag) first).getAttributeValue(StandardDialect.PREFIX, StandardTextTagProcessor.ATTR_NAME);
        if (!StringUtils.isEmpty(layout)) {
            return layout;
        }
        return titleModel.size() > 2 ? "'" + ((IText) titleModel.get(1)).getText() + "'" : null;
    }

    private final ITemplateContext context;
    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, sets up the element decorator context.
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

        // TODO: A lot of this code is just to deal with titles.  I think it should
        //       go into its own file.
        IModel sourceTitle = titleRetriever(sourceHeadModel);
        IModel targetTitle = titleRetriever(targetHeadModel);

        IAttribute titlePatternProcessor = titlePatternProcessorRetriever(sourceTitle);
        if (titlePatternProcessor == null) {
            titlePatternProcessor = titlePatternProcessorRetriever(targetTitle);
        }
        IModel resultTitle;
        if (titlePatternProcessor != null) {
            String contentTitle = titleValueRetriever(sourceTitle);
            String decoratorTitle = titleValueRetriever(targetTitle);
            Map<String, Object> map = new LinkedHashMap<>(3);
            map.put(titlePatternProcessor.getAttributeCompleteName(), titlePatternProcessor.getValue());
            map.put("data-layout-content-title", contentTitle);
            map.put("data-layout-decorator-title", decoratorTitle);
            ModelBuilder builder = new ModelBuilder(context);
            resultTitle = builder.createNode("title", map);
        } else {
            resultTitle = new ElementMerger(context.getModelFactory()).merge(targetTitle, sourceTitle);
        }

        MetaClass.insertModelWithWhitespace(targetHeadModel,
                1, resultTitle);

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
