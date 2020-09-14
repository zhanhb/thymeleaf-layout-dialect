/*
 * Copyright 2015, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.models;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.fragments.CollectFragmentProcessor;
import nz.net.ultraq.thymeleaf.fragments.FragmentProcessor;
import nz.net.ultraq.thymeleaf.internal.IContextDelegate;
import nz.net.ultraq.thymeleaf.models.extensions.IAttributeExtensions;
import nz.net.ultraq.thymeleaf.models.extensions.IModelExtensions;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.StandardWithTagProcessor;

/**
 * Merges attributes from one element into another.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class AttributeMerger implements ModelMerger {

    private final ITemplateContext context;

    /**
     * Constructor, sets up the attribute merger context.
     *
     * @param context
     */
    public AttributeMerger(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Merge the attributes of the source element with those of the target
     * element. This is basically a copy of all attributes in the source model
     * with those in the target model, overwriting any attributes that have the
     * same name, except for the case of {@code th:with} where variable
     * declarations are preserved, only overwriting same-named declarations.
     *
     * @param sourceModel
     * @param targetModel
     * @return New element with the merged attributes.
     */
    @Override
    public IModel merge(IModel targetModel, IModel sourceModel) {
        // If one of the parameters is missing return a copy of the other, or
        // nothing if both parameters are missing.
        if (!IModelExtensions.asBoolean(targetModel) || !IModelExtensions.asBoolean(sourceModel)) {
            IModel result = IModelExtensions.asBoolean(targetModel) ? targetModel.cloneModel() : null;
            return IModelExtensions.asBoolean(result) ? result : IModelExtensions.asBoolean(sourceModel) ? sourceModel.cloneModel() : null;
        }

        IModel mergedModel = targetModel.cloneModel();
        String layoutDialectPrefix = IContextDelegate.getPrefixForDialect(context, LayoutDialect.class);
        String standardDialectPrefix = IContextDelegate.getPrefixForDialect(context, StandardDialect.class);

        // Merge attributes from the source model's root event to the target model's root event
        // TODO nullable
        for (IAttribute sourceAttribute : ((IProcessableElementTag) sourceModel.get(0)).getAllAttributes()) {
            // Don't include layout:fragment processors
            if (IAttributeExtensions.equalsName(sourceAttribute, layoutDialectPrefix, FragmentProcessor.PROCESSOR_NAME)
                    || IAttributeExtensions.equalsName(sourceAttribute, layoutDialectPrefix, CollectFragmentProcessor.PROCESSOR_DEFINE)) {
                continue;
            }

            IProcessableElementTag mergedEvent = (IProcessableElementTag) IModelExtensions.first(mergedModel);
            String mergedAttributeValue; // Merge th:with attributes
            if (IAttributeExtensions.equalsName(sourceAttribute, standardDialectPrefix, StandardWithTagProcessor.ATTR_NAME)) {
                mergedAttributeValue = new VariableDeclarationMerger(context).merge(sourceAttribute.getValue(),
                        mergedEvent.getAttributeValue(standardDialectPrefix, StandardWithTagProcessor.ATTR_NAME));
            } else { // Copy every other attribute straight
                mergedAttributeValue = sourceAttribute.getValue();
            }

            mergedModel.replace(0, context.getModelFactory().replaceAttribute(mergedEvent,
                    sourceAttribute.getAttributeDefinition().getAttributeName(), sourceAttribute.getAttributeCompleteName(),
                    mergedAttributeValue));
        }

        return mergedModel;
    }

    public final ITemplateContext getContext() {
        return context;
    }

}
