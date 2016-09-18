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
package nz.net.ultraq.thymeleaf.fragments;

import java.util.concurrent.atomic.AtomicBoolean;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.models.ElementMerger;
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

/**
 * This processor serves a dual purpose: to mark sections of the template that
 * can be replaced, and to do the replacing when they're encountered.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class FragmentProcessor extends AbstractAttributeTagProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FragmentProcessor.class);

    private static final AtomicBoolean warned = new AtomicBoolean();

    public static final String PROCESSOR_NAME = "fragment";
    public static final int PROCESSOR_PRECEDENCE = 1;

    /**
     * Constructor, sets this processor to work on the 'fragment' attribute.
     *
     * @param templateMode
     * @param dialectPrefix
     */
    public FragmentProcessor(TemplateMode templateMode, String dialectPrefix) {
        super(templateMode, dialectPrefix, null, false, PROCESSOR_NAME, true, PROCESSOR_PRECEDENCE, true);
    }

    /**
     * Inserts the content of fragments into the encountered fragment
     * placeholder.
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
        // Emit a warning if found in the <head> section
        if (warned.compareAndSet(false, true) && getTemplateMode() == TemplateMode.HTML) {
            for (IProcessableElementTag element : context.getElementStack()) {
                if ("head".equals(element.getElementCompleteName())) {
                    logger.warn("You don't need to put the layout:fragment/data-layout-fragment attribute into the <head> section - "
                            + "the decoration process will automatically copy the <head> section of your content templates into your layout page.");
                    break;
                }
            }
        }

        // Locate the fragment that corresponds to this decorator/include fragment
        IModel fragment = FragmentMap.get(context).get(attributeValue);
        // Replace the tag body with the fragment
        if (MetaClass.asBoolean(fragment)) {
            IModelFactory modelFactory = context.getModelFactory();
            IModel replacementModel = new ElementMerger(context).merge(modelFactory.createModel(tag), fragment);

            // Remove the layout:fragment attribute - Thymeleaf won't do it for us
            // when using StructureHandler.replaceWith(...)
            replacementModel.replace(0, modelFactory.removeAttribute((IProcessableElementTag) MetaClass.first(replacementModel),
                    MetaClass.getPrefixForDialect(context, LayoutDialect.class), PROCESSOR_NAME));

            structureHandler.replaceWith(replacementModel, true);
        }
    }

}
