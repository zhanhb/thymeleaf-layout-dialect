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
package nz.net.ultraq.thymeleaf.includes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import nz.net.ultraq.thymeleaf.expressions.ExpressionProcessor;
import nz.net.ultraq.thymeleaf.fragments.FragmentFinder;
import nz.net.ultraq.thymeleaf.fragments.FragmentParameterNamesExtractor;
import nz.net.ultraq.thymeleaf.fragments.FragmentProcessor;
import nz.net.ultraq.thymeleaf.fragments.extensions.FragmentExtensions;
import nz.net.ultraq.thymeleaf.models.TemplateModelFinder;
import nz.net.ultraq.thymeleaf.models.extensions.ChildModelIterator;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.Assignation;
import org.thymeleaf.standard.expression.AssignationSequence;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Similar to Thymeleaf's {@code th:include}, but allows the passing of entire
 * element fragments to the included template. Useful if you have some HTML that
 * you want to reuse, but whose contents are too complex to determine or
 * construct with context variables alone.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 * @deprecated Use {@link InsertProcessor} ({@code layout:insert}) instead.
 */
@Deprecated
public class IncludeProcessor extends AbstractAttributeModelProcessor {

    private static final Logger logger = LoggerFactory.getLogger(IncludeProcessor.class);

    private static final AtomicBoolean warned = new AtomicBoolean();

    public static final String PROCESSOR_NAME = "include";
    public static final int PROCESSOR_PRECEDENCE = 0;

    /**
     * Constructor, sets this processor to work on the 'include' attribute.
     *
     * @param templateMode
     * @param dialectPrefix
     */
    public IncludeProcessor(TemplateMode templateMode, String dialectPrefix) {
        super(templateMode, dialectPrefix, null, false, PROCESSOR_NAME, true, PROCESSOR_PRECEDENCE, true);
    }

    /**
     * Locates a page fragment and includes it in the current template.
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
        if (warned.compareAndSet(false, true)) {
            logger.warn(
                    "The layout:include/data-layout-include processor is deprecated and will be removed in the next major version of the layout dialect.  "
                    + "Use the layout:insert/data-layout-insert processor instead.  "
                    + "See https://github.com/ultraq/thymeleaf-layout-dialect/issues/107 for more information."
            );
        }
        // Locate the page and fragment for inclusion
        FragmentExpression fragmentExpression = new ExpressionProcessor(context).parseFragmentExpression(attributeValue);
        TemplateModel fragmentForInclusion = new TemplateModelFinder(context).findFragment(fragmentExpression);

        // Gather all fragment parts within the include element, scoping them to this element
        Map<String, List<IModel>> includeFragments = new FragmentFinder(getDialectPrefix()).findFragments(model);
        FragmentExtensions.setLocalFragmentCollection(structureHandler, context, includeFragments);

        // Keep track of what template is being processed?  Thymeleaf does this for
        // its include processor, so I'm just doing the same here.
        structureHandler.setTemplateData(fragmentForInclusion.getTemplateData());

        // Replace the children of this element with the children of the included page fragment
        IModel fragmentForInclusionUse = fragmentForInclusion.cloneModel();
        IModelExtensions.removeChildren(model);

        // Retrieving a model for a template can come with whitspace, so trim those
        // from the model so that we can use the child event iterator.
        IModelExtensions.trim(fragmentForInclusionUse);

        ChildModelIterator it = IModelExtensions.childModelIterator(fragmentForInclusionUse);
        if (it != null) {
            while (it.hasNext()) {
                IModel fragmentChildModel = it.next();
                model.insertModel(model.size() - 1, fragmentChildModel);
            }
        }

        // When fragment parameters aren't named, derive the name from the fragment definition
        // TODO: Common code across all the inclusion processors
        if (fragmentExpression.hasSyntheticParameters()) {
            String fragmentDefinition = ((IProcessableElementTag) IModelExtensions.first(fragmentForInclusionUse)).getAttributeValue(getDialectPrefix(), FragmentProcessor.PROCESSOR_NAME);
            List<String> parameterNames = new FragmentParameterNamesExtractor().extract(fragmentDefinition);

            AssignationSequence parameters = fragmentExpression.getParameters();
            if (parameters != null) {
                int index = 0;
                for (Assignation parameter : parameters) {
                    structureHandler.setLocalVariable(parameterNames.get(index), parameter.getRight().execute(context));
                    ++index;
                }
            }
        } else { // Otherwise, apply values as is
            AssignationSequence parameters = fragmentExpression.getParameters();
            if (parameters != null) {
                for (Assignation parameter : parameters) {
                    structureHandler.setLocalVariable((String) parameter.getLeft().execute(context), parameter.getRight().execute(context));
                }
            }
        }
    }

}
