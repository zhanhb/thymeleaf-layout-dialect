/*
 * Copyright 2016, Emanuel Rabina (http://www.ultraq.net.nz/)
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

import java.util.LinkedHashMap;
import java.util.Map;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.internal.ModelBuilder;
import nz.net.ultraq.thymeleaf.models.ElementMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardTextTagProcessor;
import org.thymeleaf.util.StringUtils;
import org.unbescape.html.HtmlEscape;

/**
 * Decorator for the {@code <title>} part of the template to handle the special
 * processing required for the {@code layout:title-pattern} processor.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class HtmlTitleDecorator implements Decorator {

    // Get the title pattern to use
    private static IAttribute titlePatternProcessorRetriever(IModel titleModel, String attributeName) {
        if (!MetaClass.asBoolean(titleModel)) {
            return null;
        }
        IProcessableElementTag event = (IProcessableElementTag) MetaClass.first(titleModel);
        return event != null ? event.getAttribute(attributeName, TitlePatternProcessor.PROCESSOR_NAME) : null;
    }

    private static void titleValueExtractor(IModel titleModel, String titleAttribute, String standardDialectPrefix, Map<String, Object> titleValuesMap) {
        ITemplateEvent titleTag = MetaClass.asBoolean(titleModel) ? MetaClass.first(titleModel) : null;
        if (titleTag != null) {
            String titleValue = ((IProcessableElementTag) titleTag).getAttributeValue(titleAttribute);
            if (StringUtils.isEmpty(titleValue)) {
                titleValue = ((IProcessableElementTag) titleTag).getAttributeValue(standardDialectPrefix, StandardTextTagProcessor.ATTR_NAME);
            }
            if (StringUtils.isEmpty(titleValue)) {
                titleValue = MetaClass.asBoolean(titleModel) && titleModel.size() > 2 ? "'"
                        + HtmlEscape.escapeHtml5Xml(((IText) titleModel.get(1)).getText())
                        + "'" : null;
            }
            if (!StringUtils.isEmpty(titleValue)) {
                titleValuesMap.put(titleAttribute, titleValue);
            }
        }
    }

    private final ITemplateContext context;

    /**
     * Constructor, sets up the decorator context.
     *
     * @param context
     */
    public HtmlTitleDecorator(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Special decorator for the {@code <title>} part, accumulates the important
     * processing parts for the {@code layout:title-pattern} processor.
     *
     * @param targetTitleModel
     * @param sourceTitleModel
     * @return A new {@code <title>} model that is the result of decorating the
     * {@code <title>}.
     */
    @Override
    public IModel decorate(IModel targetTitleModel, IModel sourceTitleModel) {
        String layoutDialectPrefix = MetaClass.getPrefixForDialect(context, LayoutDialect.class);
        String standardDialectPrefix = MetaClass.getPrefixForDialect(context, StandardDialect.class);

        IAttribute titlePatternProcessor = titlePatternProcessorRetriever(sourceTitleModel, layoutDialectPrefix);
        if (titlePatternProcessor == null) {
            titlePatternProcessor = titlePatternProcessorRetriever(targetTitleModel, layoutDialectPrefix);
        }

        IModel resultTitle;

        // Set the title pattern to use on a new model, as well as the important
        // title result parts that we want to use on the pattern.
        if (titlePatternProcessor != null) {
            LinkedHashMap<String, Object> titleValuesMap = new LinkedHashMap<>(2);
            titleValueExtractor(sourceTitleModel, TitlePatternProcessor.CONTENT_TITLE_ATTRIBUTE, standardDialectPrefix, titleValuesMap);
            titleValueExtractor(targetTitleModel, TitlePatternProcessor.LAYOUT_TITLE_ATTRIBUTE, standardDialectPrefix, titleValuesMap);

            LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>(3);
            linkedHashMap.put(titlePatternProcessor.getAttributeCompleteName(), titlePatternProcessor.getValue());
            linkedHashMap.putAll(titleValuesMap);
            resultTitle = new ModelBuilder(context).createNode("title", linkedHashMap);
        } else {
            resultTitle = new ElementMerger(context).merge(targetTitleModel, sourceTitleModel);
        }
        return resultTitle;
    }

}
