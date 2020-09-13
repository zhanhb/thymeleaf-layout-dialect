/*
 * Copyright 2017, Emanuel Rabina (http://www.ultraq.net.nz/)
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
import java.util.concurrent.atomic.AtomicBoolean;
import nz.net.ultraq.thymeleaf.internal.Extensions;
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
import org.thymeleaf.util.StringUtils;

/**
 * Processor produced from FragmentProcessor in order to separate include and
 * define logic to avoid ambiguity.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 * @author George Vinokhodov
 */
public class CollectFragmentProcessor extends AbstractAttributeTagProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CollectFragmentProcessor.class);

    private static final AtomicBoolean warned = new AtomicBoolean();

    public static final String PROCESSOR_DEFINE = "define";
    public static final String PROCESSOR_COLLECT = "collect";
    public static final int PROCESSOR_PRECEDENCE = 1;

    /**
     * Constructor, sets this processor to work on the 'collect' attribute.
     *
     * @param templateMode
     * @param dialectPrefix
     */
    public CollectFragmentProcessor(TemplateMode templateMode, String dialectPrefix) {

        super(templateMode, dialectPrefix, null, false, PROCESSOR_COLLECT, true, PROCESSOR_PRECEDENCE, true);
    }

    /**
     * Inserts the content of <code>:define</code> fragments into the
     * encountered collect placeholder.
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
        if (getTemplateMode() == TemplateMode.HTML) {
            for (IProcessableElementTag element : context.getElementStack()) {
                if ("head".equals(element.getElementCompleteName())) {
                    if (warned.compareAndSet(false, true)) {
                        logger.warn(
                                "You don\'t need to put the layout:fragment/data-layout-fragment attribute into the <head> section - "
                                + "the decoration process will automatically copy the <head> section of your content templates into your layout page."
                        );
                    }
                    break;
                }
            }
        }

        // All :define fragments we collected, :collect fragments included to determine where to stop.
        // Fragments after :collect are preserved for the next :collect event
        List<IModel> fragments = FragmentMap.get(context).get(attributeValue);

        // Replace the tag body with the fragment
        if (fragments != null && !fragments.isEmpty()) {
            IModelFactory modelFactory = context.getModelFactory();
            ElementMerger merger = new ElementMerger(context);
            IModel[] replacementModel = new IModel[]{modelFactory.createModel(tag)};
            boolean first = true;
            while (!fragments.isEmpty()) {
                IModel fragment = fragments.remove(0);
                if (!StringUtils.isEmpty(((IProcessableElementTag) fragment.get(0)).getAttributeValue(getDialectPrefix(), PROCESSOR_COLLECT))) {
                    break;
                }
                if (first) {
                    replacementModel[0] = merger.merge(replacementModel[0], fragment);
                    first = false;
                } else {
                    AtomicBoolean firstEvent = new AtomicBoolean(true);
                    Extensions.each(fragment, event -> {
                        if (firstEvent.compareAndSet(true, false)) {
                            replacementModel[0].add(modelFactory.createText("\n"));
                            replacementModel[0].add(modelFactory.removeAttribute((IProcessableElementTag) event, getDialectPrefix(), PROCESSOR_DEFINE));
                        } else {
                            replacementModel[0].add(event);
                        }
                    });
                }
            }

            // Remove the layout:collect attribute - Thymeleaf won't do it for us
            // when using StructureHandler.replaceWith(...)
            replacementModel[0].replace(0, modelFactory.removeAttribute((IProcessableElementTag) Extensions.first(replacementModel[0]), getDialectPrefix(), PROCESSOR_COLLECT));

            structureHandler.replaceWith(replacementModel[0], true);
        }
    }

}
