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
import nz.net.ultraq.thymeleaf.internal.Extensions;
import nz.net.ultraq.thymeleaf.internal.ModelBuilder;
import nz.net.ultraq.thymeleaf.models.ElementMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IText;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardTextTagProcessor;
import org.thymeleaf.standard.processor.StandardUtextTagProcessor;
import org.unbescape.html.HtmlEscape;

import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.CONTENT_TITLE_ATTRIBUTE;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.CONTENT_TITLE_ATTRIBUTE_UNESCAPED;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.LAYOUT_TITLE_ATTRIBUTE;
import static nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor.LAYOUT_TITLE_ATTRIBUTE_UNESCAPED;

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
        if (!Extensions.asBoolean(titleModel)) {
            return null;
        }
        IProcessableElementTag event = (IProcessableElementTag) Extensions.first(titleModel);
        return event != null ? event.getAttribute(attributeName, TitlePatternProcessor.PROCESSOR_NAME) : null;
    }

    private static void titleValueExtractor(IModel titleModel,
            String titleAttribute,
            String titleAttributeUnescaped,
            String standardDialectPrefix,
            Map<String, ? super String> titleValuesMap) {
        IProcessableElementTag titleTag = Extensions.asBoolean(titleModel) ? (IProcessableElementTag) Extensions.first(titleModel) : null;
        if (titleTag != null) {
            if (titleTag.hasAttribute(titleAttribute)) {
                titleValuesMap.put(titleAttribute, titleTag.getAttributeValue(titleAttribute));
            } else if (titleTag.hasAttribute(standardDialectPrefix, StandardTextTagProcessor.ATTR_NAME)) {
                titleValuesMap.put(titleAttribute,
                        titleTag.getAttributeValue(standardDialectPrefix, StandardTextTagProcessor.ATTR_NAME));
            } else if (titleTag.hasAttribute(standardDialectPrefix, StandardUtextTagProcessor.ATTR_NAME)) {
                titleValuesMap.put(titleAttributeUnescaped,
                        titleTag.getAttributeValue(standardDialectPrefix, StandardUtextTagProcessor.ATTR_NAME));
            } else if (titleModel != null && titleModel.size() > 2) {
                titleValuesMap.put(titleAttributeUnescaped, "'"
                        + HtmlEscape.escapeHtml5Xml(((IText) titleModel.get(1)).getText())
                        + "'");
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
        String layoutDialectPrefix = Extensions.getPrefixForDialect(context, LayoutDialect.class);
        String standardDialectPrefix = Extensions.getPrefixForDialect(context, StandardDialect.class);

        IAttribute titlePatternProcessor = titlePatternProcessorRetriever(sourceTitleModel, layoutDialectPrefix);
        if (titlePatternProcessor == null) {
            titlePatternProcessor = titlePatternProcessorRetriever(targetTitleModel, layoutDialectPrefix);
        }

        IModel resultTitle;

        // Set the title pattern to use on a new model, as well as the important
        // title result parts that we want to use on the pattern.
        if (titlePatternProcessor != null) {
            // TODO: This title values map is being used as a way to communicate
            //       between this class and the title pattern processor, and being
            //       exposed on the Thymeleaf model as a result.  I should find a
            //       better way of passing these values around, maybe via the layout
            //       context.
            LinkedHashMap<String, String> titleValuesMap = new LinkedHashMap<>(3);

            titleValuesMap.put(titlePatternProcessor.getAttributeCompleteName(), titlePatternProcessor.getValue());
            titleValueExtractor(sourceTitleModel, CONTENT_TITLE_ATTRIBUTE, CONTENT_TITLE_ATTRIBUTE_UNESCAPED, standardDialectPrefix, titleValuesMap);
            titleValueExtractor(targetTitleModel, LAYOUT_TITLE_ATTRIBUTE, LAYOUT_TITLE_ATTRIBUTE_UNESCAPED, standardDialectPrefix, titleValuesMap);
            resultTitle = new ModelBuilder(context).createNode("title", titleValuesMap);
        } else {
            resultTitle = new ElementMerger(context).merge(targetTitleModel, sourceTitleModel);
        }
        return resultTitle;
    }

}
