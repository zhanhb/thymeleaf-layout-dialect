/*
 * Copyright 2019, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.fragments;

import java.util.List;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.expression.Assignation;
import org.thymeleaf.standard.expression.AssignationSequence;
import org.thymeleaf.standard.expression.FragmentExpression;

/**
 * Updates the variables at a given element/fragment scope to include those in a
 * fragment expression.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class FragmentParameterVariableUpdater {

    private final String dialectPrefix;
    private final ITemplateContext context;

    /**
     * Constructor, set the dialect prefix currently being used.
     *
     * @param dialectPrefix
     */
    public FragmentParameterVariableUpdater(String dialectPrefix, ITemplateContext context) {

        this.dialectPrefix = dialectPrefix;
        this.context = context;
    }

    /**
     * Given a fragment expression, update the local variables of the element
     * being processed.
     *
     * @param fragmentExpression
     * @param fragment
     * @param structureHandler
     */
    public void updateLocalVariables(FragmentExpression fragmentExpression, IModel fragment,
            IElementModelStructureHandler structureHandler) {

        // When fragment parameters aren't named, derive the name from the fragment definition
        if (fragmentExpression.hasSyntheticParameters()) {
            String fragmentDefinition = ((IProcessableElementTag) IModelExtensions.first(fragment)).getAttributeValue(dialectPrefix, FragmentProcessor.PROCESSOR_NAME);
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

    public final String getDialectPrefix() {
        return this.dialectPrefix;
    }

    public final ITemplateContext getContext() {
        return this.context;
    }

}
