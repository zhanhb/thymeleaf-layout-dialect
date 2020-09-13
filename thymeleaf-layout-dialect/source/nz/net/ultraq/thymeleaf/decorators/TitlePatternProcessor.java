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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.StringUtils;

/**
 * Allows for greater control of the resulting {@code <title>} element by
 * specifying a pattern with some special tokens. This can be used to extend the
 * layout's title with the content's one, instead of simply overriding it.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class TitlePatternProcessor extends AbstractAttributeTagProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TitlePatternProcessor.class);

    @Deprecated
    private static final String TOKEN_DECORATOR_TITLE = "$DECORATOR_TITLE";
    // private static final String TOKEN_CONTENT_TITLE   = "$CONTENT_TITLE";
    private static final String TOKEN_LAYOUT_TITLE = "$LAYOUT_TITLE";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\$(LAYOUT|DECORATOR|CONTENT)_TITLE)");

    private static final AtomicBoolean warned = new AtomicBoolean();

    public static final String PROCESSOR_NAME = "title-pattern";
    public static final int PROCESSOR_PRECEDENCE = 1;

    public static final String CONTENT_TITLE_KEY = "LayoutDialect::ContentTitle";
    public static final String LAYOUT_TITLE_KEY = "LayoutDialect::LayoutTitle";

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
     * text with the titles from the content and layout pages.
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
        IModelFactory modelFactory = context.getModelFactory();

        IModel contentTitle = (IModel) context.getVariable(CONTENT_TITLE_KEY);
        IModel layoutTitle = (IModel) context.getVariable(LAYOUT_TITLE_KEY);

        if (!StringUtils.isEmpty(titlePattern) && titlePattern.contains(TOKEN_DECORATOR_TITLE)) {
            if (warned.compareAndSet(false, true)) {
                logger.warn(
                        "The $DECORATOR_TITLE token is deprecated and will be removed in the next major version of the layout dialect.  "
                        + "Please use the $LAYOUT_TITLE token instead to future-proof your code.  "
                        + "See https://github.com/ultraq/thymeleaf-layout-dialect/issues/95 for more information."
                );
            }
        }

        // Break the title pattern up into tokens to map to their respective models
        IModel titleModel = modelFactory.createModel();
        if (IModelExtensions.asBoolean(layoutTitle) && IModelExtensions.asBoolean(contentTitle)) {
            Matcher matcher = TOKEN_PATTERN.matcher(titlePattern);
            while (matcher.find()) {
                String text = titlePattern.substring(matcher.regionStart(), matcher.start());
                if (!StringUtils.isEmpty(text)) {
                    titleModel.add(modelFactory.createText(text));
                }
                String token = matcher.group(1);
                titleModel.addModel(TOKEN_LAYOUT_TITLE.equals(token) || TOKEN_DECORATOR_TITLE.equals(token) ? layoutTitle : contentTitle);
                matcher.region(matcher.regionStart() + text.length() + token.length(), titlePattern.length());
            }
            String remainingText = titlePattern.substring(matcher.regionStart());
            if (!StringUtils.isEmpty(remainingText)) {
                titleModel.add(modelFactory.createText(remainingText));
            }
        } else if (IModelExtensions.asBoolean(contentTitle)) {
            titleModel.addModel(contentTitle);
        } else if (IModelExtensions.asBoolean(layoutTitle)) {
            titleModel.addModel(layoutTitle);
        }

        structureHandler.setBody(titleModel, true);
    }

}
