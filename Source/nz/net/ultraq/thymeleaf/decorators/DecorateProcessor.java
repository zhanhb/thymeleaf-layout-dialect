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

import java.util.Map;
import nz.net.ultraq.thymeleaf.decorators.html.HtmlDocumentDecorator;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlDocumentDecorator;
import nz.net.ultraq.thymeleaf.expressions.ExpressionProcessor;
import nz.net.ultraq.thymeleaf.fragments.FragmentFinder;
import nz.net.ultraq.thymeleaf.fragments.FragmentMap;
import nz.net.ultraq.thymeleaf.models.TemplateModelFinder;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Specifies the name of the template to decorate using the current template.
 *
 * @author Emanuel Rabina
 */
@lombok.experimental.ExtensionMethod(nz.net.ultraq.thymeleaf.internal.MetaClass.class)
public class DecorateProcessor extends AbstractAttributeModelProcessor {

    public static final String PROCESSOR_NAME = "decorate";
    public static final int PROCESSOR_PRECEDENCE = 0;

    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, configure this processor to work on the 'decorate' attribute
     * and to use the given sorting strategy.
     *
     * @param templateMode
     * @param dialectPrefix
     * @param sortingStrategy
     */
    public DecorateProcessor(TemplateMode templateMode, String dialectPrefix, SortingStrategy sortingStrategy) {
        this(templateMode, dialectPrefix, sortingStrategy, PROCESSOR_NAME);
    }

    /**
     * Constructor, configurable processor name so that I could support the
     * deprecated {@code layout:decorator} alias.
     *
     * @param templateMode
     * @param dialectPrefix
     * @param sortingStrategy
     * @param attributeName
     */
    protected DecorateProcessor(TemplateMode templateMode, String dialectPrefix, SortingStrategy sortingStrategy,
            String attributeName) {
        super(templateMode, dialectPrefix, null, false, attributeName, true, PROCESSOR_PRECEDENCE, false);
        this.sortingStrategy = sortingStrategy;
    }

    /**
     * Locates the template to decorate and, once decorated, inserts it into the
     * processing chain.
     *
     * @param context
     * @param model
     * @param attributeName
     * @param attributeValue
     * @param structureHandler
     */
    @Override
    protected void doProcess(ITemplateContext context, IModel model, AttributeName attributeName,
            String attributeValue, IElementModelStructureHandler structureHandler) {

        // Ensure that every element to this point contained a decorate processor
        for (IProcessableElementTag element : context.getElementStack()) {
            if (element.getAttribute(attributeName) == null) {
                throw new IllegalArgumentException("layout:decorate/data-layout-decorate must appear in the root element of your template");
            }
        }

        TemplateModelFinder templateModelFinder = new TemplateModelFinder(context);

        // Remove the decorate processor from the root element
        IProcessableElementTag rootElement = (IProcessableElementTag) model.first();
        if (rootElement.hasAttribute(attributeName)) {
            rootElement = context.getModelFactory().removeAttribute(rootElement, attributeName);
            model.replace(0, rootElement);
        }

        // Load the entirety of this template
        // TODO: Can probably find a way of preventing this double-loading for #102
        String contentTemplateName = context.getTemplateData().getTemplate();
        IModel contentTemplate = templateModelFinder.findTemplate(contentTemplateName).cloneModel();
        contentTemplate.replace(contentTemplate.findIndexOf(event -> event instanceof IOpenElementTag), rootElement);

        // Locate the template to decorate
        FragmentExpression decorateTemplateExpression = new ExpressionProcessor(context).parseFragmentExpression(attributeValue);
        IModel decorateTemplate = templateModelFinder.findTemplate(decorateTemplateExpression).cloneModel();

        // Gather all fragment parts from this page to apply to the new document
        // after decoration has taken place
        Map<String, IModel> pageFragments = new FragmentFinder(getDialectPrefix()).findFragments(model);

        // Choose the decorator to use based on template mode, then apply it
        TemplateMode templateMode = getTemplateMode();
        XmlDocumentDecorator decorator
                = templateMode == TemplateMode.HTML ? new HtmlDocumentDecorator(context, sortingStrategy)
                        : templateMode == TemplateMode.XML ? new XmlDocumentDecorator(context)
                                : null;
        if (decorator == null) {
            throw new IllegalArgumentException(
                    "Layout dialect cannot be applied to the " + templateMode + " template mode, only HTML and XML template modes are currently supported"
            );
        }
        IModel resultTemplate = decorator.decorate(decorateTemplate, contentTemplate);
        model.replaceModel(0, resultTemplate);

        // Save layout fragments for use later by layout:fragment processors
        FragmentMap.setForNode(context, structureHandler, pageFragments);
    }

}
