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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import nz.net.ultraq.thymeleaf.decorators.html.HtmlDocumentDecorator;
import nz.net.ultraq.thymeleaf.decorators.xml.XmlDocumentDecorator;
import nz.net.ultraq.thymeleaf.expressions.ExpressionProcessor;
import nz.net.ultraq.thymeleaf.fragments.FragmentFinder;
import nz.net.ultraq.thymeleaf.fragments.extensions.FragmentExtensions;
import nz.net.ultraq.thymeleaf.internal.IContextDelegate;
import nz.net.ultraq.thymeleaf.models.TemplateModelFinder;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.TemplateData;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.Assignation;
import org.thymeleaf.standard.expression.AssignationSequence;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Specifies the name of the template to decorate using the current template.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class DecorateProcessor extends AbstractAttributeModelProcessor {

    public static final String PROCESSOR_NAME = "decorate";
    public static final int PROCESSOR_PRECEDENCE = 0;

    /**
     * Compare the root elements, barring some attributes, to see if they are
     * the same.
     *
     * @param element1
     * @param element2
     * @param context
     * @return {@code true} if the elements share the same name and all
     * attributes, with the exception of XML namespace declarations and
     * Thymeleaf's {@code th:with} attribute processor.
     */
    private static boolean rootElementsEqual(IProcessableElementTag element1,
            IProcessableElementTag element2, IContext context) {

        if (element1 != null && element2 != null
                && Objects.equals(element1.getElementDefinition(), element2.getElementDefinition())) {
            String maybe = IContextDelegate.getPrefixForDialect(context, StandardDialect.class) + ":with";
            Map<String, String> attributeMap = element2.getAttributeMap();
            for (Map.Entry<String, String> entry : element1.getAttributeMap().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.startsWith("xmlns:") || key.equals(maybe)) {
                    continue;
                }
                if (!attributeMap.containsKey(key)) {
                    return false;
                }
                if (!Objects.equals(value, attributeMap.get(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private final boolean autoHeadMerging;
    private final SortingStrategy sortingStrategy;

    /**
     * Constructor, configure this processor to work on the 'decorate' attribute
     * and to use the given sorting strategy.
     *
     * @param templateMode
     * @param dialectPrefix
     * @param sortingStrategy
     * @param autoHeadMerging
     */
    public DecorateProcessor(TemplateMode templateMode, String dialectPrefix, SortingStrategy sortingStrategy,
            boolean autoHeadMerging) {
        this(templateMode, dialectPrefix, sortingStrategy, autoHeadMerging, PROCESSOR_NAME);
    }

    /**
     * Constructor, configurable processor name so that I could support the
     * deprecated {@code layout:decorator} alias.
     *
     * @param templateMode
     * @param dialectPrefix
     * @param sortingStrategy
     * @param autoHeadMerging
     * @param attributeName
     */
    protected DecorateProcessor(TemplateMode templateMode, String dialectPrefix, SortingStrategy sortingStrategy,
            boolean autoHeadMerging, String attributeName) {
        super(templateMode, dialectPrefix, null, false, attributeName, true, PROCESSOR_PRECEDENCE, false);
        this.sortingStrategy = sortingStrategy;
        this.autoHeadMerging = autoHeadMerging;
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

        TemplateModelFinder templateModelFinder = new TemplateModelFinder(context);

        // Load the entirety of this template so we can access items outside of the root element
        String contentTemplateName = context.getTemplateData().getTemplate();
        IModel contentTemplate = templateModelFinder.findTemplate(contentTemplateName).cloneModel();

        // Check that the root element is the same as the one currently being processed
        IProcessableElementTag contentRootEvent = (IProcessableElementTag) IModelExtensions.find(contentTemplate, event -> event instanceof IProcessableElementTag);
        IProcessableElementTag rootElement = (IProcessableElementTag) IModelExtensions.first(model);
        if (!rootElementsEqual(contentRootEvent, rootElement, context)) {
            throw new IllegalArgumentException("layout:decorate/data-layout-decorate must appear in the root element of your template");
        }

        // Remove the decorate processor from the root element
        if (rootElement.hasAttribute(attributeName)) {
            rootElement = context.getModelFactory().removeAttribute(rootElement, attributeName);
            model.replace(0, rootElement);
        }
        IModelExtensions.replaceModel(contentTemplate, IModelExtensions.findIndexOf(contentTemplate, event -> event instanceof IProcessableElementTag), model);

        // Locate the template to decorate
        FragmentExpression decorateTemplateExpression = new ExpressionProcessor(context).parseFragmentExpression(attributeValue);
        TemplateModel decorateTemplate = templateModelFinder.findTemplate(decorateTemplateExpression);
        TemplateData decorateTemplateData = decorateTemplate.getTemplateData();
        IModel clone = decorateTemplate.cloneModel();

        // Gather all fragment parts from this page to apply to the new document
        // after decoration has taken place
        Map<String, List<IModel>> pageFragments = new FragmentFinder(getDialectPrefix()).findFragments(model);

        // Choose the decorator to use based on template mode, then apply it
        TemplateMode templateMode = getTemplateMode();
        XmlDocumentDecorator decorator
                = templateMode == TemplateMode.HTML ? new HtmlDocumentDecorator(context, sortingStrategy, autoHeadMerging)
                        : templateMode == TemplateMode.XML ? new XmlDocumentDecorator(context)
                                : null;
        if (decorator == null) {
            throw new IllegalArgumentException(
                    "Layout dialect cannot be applied to the " + templateMode + " template mode, only HTML and XML template modes are currently supported"
            );
        }
        IModel resultTemplate = decorator.decorate(clone, contentTemplate);
        IModelExtensions.replaceModel(model, 0, resultTemplate);
        structureHandler.setTemplateData(decorateTemplateData);

        // Save layout fragments for use later by layout:fragment processors
        FragmentExtensions.setLocalFragmentCollection(structureHandler, context, pageFragments);

        // Scope variables in fragment definition to template.  Parameters *must* be
        // named as there is no mechanism for setting their name at the target
        // layout/template.
        if (decorateTemplateExpression.hasParameters()) {
            if (decorateTemplateExpression.hasSyntheticParameters()) {
                throw new IllegalArgumentException("Fragment parameters must be named when used with layout:decorate/data-layout-decorate");
            }
            AssignationSequence parameters = decorateTemplateExpression.getParameters();
            if (parameters != null) {
                for (Assignation parameter : parameters) {
                    structureHandler.setLocalVariable((String) parameter.getLeft().execute(context), parameter.getRight().execute(context));
                }
            }
        }
    }

}
