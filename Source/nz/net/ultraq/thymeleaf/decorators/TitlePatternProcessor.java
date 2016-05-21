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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import nz.net.ultraq.thymeleaf.LayoutDialectContext;
import nz.net.ultraq.thymeleaf.fragments.mergers.AttributeMerger;
import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.NestableNode;
import org.thymeleaf.dom.Text;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;
import org.thymeleaf.util.StringUtils;

/**
 * Allows for greater control of the resulting {@code <title>} element by
 * specifying a pattern with some special tokens. This can be used to extend the
 * decorator's title with the content's one, instead of simply overriding it.
 *
 * @author Emanuel Rabina
 */
public class TitlePatternProcessor extends AbstractAttrProcessor {

    private static final String PARAM_TITLE_DECORATOR = "$DECORATOR_TITLE";
    private static final String PARAM_TITLE_CONTENT = "$CONTENT_TITLE";

    public static final String PROCESSOR_NAME_TITLEPATTERN = "title-pattern";

    public static final String TITLE_TYPE = "LayoutDialect::TitlePattern::Type";
    public static final String TITLE_TYPE_DECORATOR = "decorator-title";
    public static final String TITLE_TYPE_CONTENT = "content-title";

    public static final String RESULTING_TITLE = "resultingTitle";

    /**
     * Constructor, sets this processor to work on the 'title-pattern'
     * attribute.
     */
    public TitlePatternProcessor() {
        super(PROCESSOR_NAME_TITLEPATTERN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProcessorResult processAttribute(Arguments arguments, Element element, String attributeName) {

        // Ensure this attribute is only on the <title> element
        if (!"title".equals(element.getNormalizedName())) {
            throw new IllegalArgumentException(attributeName + " processor should only appear in a <title> element");
        }

        // Retrieve title values from the expanded <title> sections within this
        // processing container (if any)
        String titlePattern = element.getAttributeValue(attributeName);
        NestableNode titleContainer = element.getParent();
        List<Element> titleElements = titleContainer != null ? titleContainer.getElementChildren() : Collections.emptyList();
        element.removeAttribute(attributeName);

        Function<String, Predicate<Element>> findTitleType = titleType -> {
            return childElement
                    -> Objects.equals(childElement.getNodeProperty(TITLE_TYPE), titleType);
        };
        Optional<Element> decoratorTitleElement = titleElements.stream().filter(findTitleType.apply(TITLE_TYPE_DECORATOR)).findFirst();
        Optional<String> decoratorTitle = decoratorTitleElement.map(Element::getFirstChild).map(node -> ((Text) node).getContent());
        Optional<Element> contentTitleElement = titleElements.stream().filter(findTitleType.apply(TITLE_TYPE_CONTENT)).findFirst();
        Optional<String> contentTitle = contentTitleElement.map(Element::getFirstChild).map(node -> ((Text) node).getContent());

        AttributeMerger attributeMerger = new AttributeMerger();
        attributeMerger.merge(element, decoratorTitleElement.orElse(null));
        attributeMerger.merge(element, contentTitleElement.orElse(null));

        String title = !StringUtils.isEmpty(titlePattern) && !StringUtils.isEmpty(decoratorTitle.orElse(null)) && !StringUtils.isEmpty(contentTitle.orElse(null))
                ? titlePattern
                .replace(PARAM_TITLE_DECORATOR, decoratorTitle.orElse(null))
                .replace(PARAM_TITLE_CONTENT, contentTitle.orElse(null))
                : contentTitle.orElse(decoratorTitle.orElse(""));

        // If there's a title, bring it up
        if (!StringUtils.isEmpty(title)) {
            element.addChild(new Text(title));
            titleContainer.getParent().insertAfter(titleContainer, element.cloneNode(null, false));
            LayoutDialectContext.forContext(arguments.getContext()).put(RESULTING_TITLE, title);
        }

        // Remove the processing section
        titleContainer.getParent().removeChild(titleContainer);

        return ProcessorResult.OK;
    }

    @Override
    public int getPrecedence() {
        return 1;
    }

}
