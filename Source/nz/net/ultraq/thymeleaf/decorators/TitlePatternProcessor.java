/*
 * Copyright 2012, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.decorators;

import nz.net.ultraq.thymeleaf.expressions.ExpressionProcessor;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.StringUtils;

/**
 * Allows for greater control of the resulting {@code <title>} element by
 * specifying a pattern with some special tokens. This can be used to extend the
 * decorator's title with the content's one, instead of simply overriding it.
 *
 * @author Emanuel Rabina
 */
public class TitlePatternProcessor extends AbstractAttributeTagProcessor {

    private static final String PARAM_TITLE_DECORATOR = "$DECORATOR_TITLE";
    private static final String PARAM_TITLE_CONTENT = "$CONTENT_TITLE";

    public static final String PROCESSOR_NAME = "title-pattern";
    public static final int PROCESSOR_PRECEDENCE = 1;

    public static final String TITLE_TYPE = "LayoutDialect::TitlePattern::Type";
    public static final String TITLE_TYPE_DECORATOR = "decorator-title";
    public static final String TITLE_TYPE_CONTENT = "content-title";

    public static final String RESULTING_TITLE = "resultingTitle";

    public static final String CONTENT_TITLE_ATTRIBUTE = "data-layout-content-title";
    public static final String DECORATOR_TITLE_ATTRIBUTE = "data-layout-decorator-title";

    private static String titleProcessor(String dataAttributeName, IProcessableElementTag tag, IElementTagStructureHandler structureHandler, ExpressionProcessor expressionProcessor) {
        String titleExpression = tag.getAttributeValue(dataAttributeName);
        if (!StringUtils.isEmpty(titleExpression)) {
            structureHandler.removeAttribute(dataAttributeName);
            return expressionProcessor.processAsString(titleExpression);
        }
        return null;
    }

    /**
     * Constructor, sets this processor to work on the 'title-pattern'
     * attribute.
     *
     * @param templateMode
     * @param dialectPrefix
     */
    public TitlePatternProcessor(TemplateMode templateMode, String dialectPrefix) {
        super(templateMode, dialectPrefix, null, false, PROCESSOR_NAME, true, PROCESSOR_PRECEDENCE, true);
    }

    /**
     * Process the {@code layout:title-pattern} directive, replaces the title
     * text with the titles from the content and decorator pages.
     *
     * @param context
     * @param tag
     * @param attributeName
     * @param attributeValue
     * @param structureHandler
     */
    @Override
    protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
            AttributeName attributeName, String attributeValue, IElementTagStructureHandler structureHandler) {

        // Ensure this attribute is only on the <title> element
        if (!"title".equals(tag.getElementCompleteName())) {
            throw new IllegalArgumentException(attributeName + " processor should only appear in a <title> element");
        }

        String titlePattern = attributeValue;
        ExpressionProcessor expressionProcessor = new ExpressionProcessor(context);

        String contentTitle = titleProcessor(CONTENT_TITLE_ATTRIBUTE, tag, structureHandler, expressionProcessor);
        String decoratorTitle = titleProcessor(DECORATOR_TITLE_ATTRIBUTE, tag, structureHandler, expressionProcessor);

        String title = !StringUtils.isEmpty(titlePattern) && !StringUtils.isEmpty(decoratorTitle) && !StringUtils.isEmpty(contentTitle)
                ? titlePattern
                .replace(PARAM_TITLE_DECORATOR, decoratorTitle)
                .replace(PARAM_TITLE_CONTENT, contentTitle)
                : !StringUtils.isEmpty(contentTitle) ? contentTitle : !StringUtils.isEmpty(decoratorTitle) ? decoratorTitle : "";

        structureHandler.setBody(title, false);
    }

}
