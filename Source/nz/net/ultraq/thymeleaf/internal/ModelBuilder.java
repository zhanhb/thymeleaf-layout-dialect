/*
 * Copyright 2016, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.internal;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.ElementDefinitions;
import org.thymeleaf.engine.HTMLElementDefinition;
import org.thymeleaf.engine.HTMLElementType;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.StringUtils;

/**
 * Create Thymeleaf 3.0 models using a simplified syntax.
 *
 * @author Emanuel Rabina
 */
public class ModelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ModelBuilder.class);

    private final ElementDefinitions elementDefinitions;
    private final IModelFactory modelFactory;
    private final TemplateMode templateMode;

    /**
     * Constructor, create a new model builder.
     *
     * @param context
     */
    public ModelBuilder(ITemplateContext context) {
        this(context.getModelFactory(), context.getConfiguration().getElementDefinitions(), context.getTemplateMode());
    }

    /**
     * Constructor, create a new model builder.
     *
     * @param modelFactory
     * @param elementDefinitions
     * @param templateMode
     */
    public ModelBuilder(IModelFactory modelFactory, ElementDefinitions elementDefinitions, TemplateMode templateMode) {
        this.modelFactory = modelFactory;
        this.elementDefinitions = elementDefinitions;
        this.templateMode = templateMode;
    }

    /**
     * Create a model for the given HTML element.
     *
     * @param name HTML element name.
     * @return New model with the given name.
     */
    public IModel createNode(Object name) {
        return createNode(name, null, null);
    }

    /**
     * Create a model for the given HTML element and inner text content.
     *
     * @param name HTML element name.
     * @param value Text content.
     * @return New model with the given name and content.
     */
    public IModel createNode(Object name, Object value) {
        return createNode(name, null, value);
    }

    /**
     * Create a model for the given HTML element and attributes.
     *
     * @param name HTML element name.
     * @param attributes Element attributes.
     * @return New model with the given name and attributes.
     */
    @SuppressWarnings("rawtypes")
    public IModel createNode(Object name, Map attributes) {
        return createNode(name, attributes, null);
    }

    /**
     * Create a model for the given HTML element, attributes, and inner text
     * content.
     *
     * @param name HTML element name.
     * @param attributes Element attributes.
     * @param value Text content.
     * @return New model with the given name, attributes, and content.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public IModel createNode(Object name, Map attributes, Object value) {
        // Normalize values for Java implementations as the model factory doesn't
        // know what to do with Groovy versions of things
        String elementName = String.valueOf(name);
        String elementText = value != null ? value.toString() : null;
        if (attributes != null) {
            Map<?, Object> map = attributes;
            for (Map.Entry<?, Object> entry : map.entrySet()) {
                entry.setValue(String.valueOf(entry.getValue()));
            }
        }

        IModel model = modelFactory.createModel();
        HTMLElementDefinition elementDefinition = (HTMLElementDefinition) elementDefinitions.forName(templateMode, elementName);

        // Standalone element
        if (elementDefinition.getType() == HTMLElementType.VOID) {
            if (attributes != null && attributes.containsKey("standalone")) {
                attributes.remove("standalone");
                model.add(modelFactory.createStandaloneElementTag(elementName, attributes, AttributeValueQuotes.DOUBLE, false, true));
            } else if (attributes != null && attributes.containsKey("void")) {
                attributes.remove("void");
                model.add(modelFactory.createStandaloneElementTag(elementName, attributes, AttributeValueQuotes.DOUBLE, false, false));
            } else {
                logger.warn("Instructed to write a closing tag {} for an HTML void element.  This "
                        + "might cause processing errors further down the track.  To avoid this, "
                        + "either self close the opening element, remove the closing tag, or "
                        + "process this template using the XML processing mode.  See "
                        + "https://html.spec.whatwg.org/multipage/syntax.html#void-elements "
                        + "for more information on HTML void elements.", name);

                model.add(modelFactory.createStandaloneElementTag(elementName, attributes, AttributeValueQuotes.DOUBLE, false, false));
                model.add(modelFactory.createCloseElementTag(elementName));
            }
        } else { // Open/close element and potential text content
            model.add(modelFactory.createOpenElementTag(elementName, attributes, AttributeValueQuotes.DOUBLE, false));
            if (!StringUtils.isEmpty(elementText)) {
                model.add(modelFactory.createText(elementText));
            }
            model.add(modelFactory.createCloseElementTag(elementName));
        }

        return model;
    }

    /**
     * Link a parent and child node. A child node is appended to a parent by
     * being the last sub-model before the parent close tag.
     *
     * @param parent
     * @param child
     */
    public void nodeCompleted(Object parent, Object child) {
        IModel parentModel = (IModel) parent;
        if (parentModel != null) {

            // TODO: Insert w/ whitespace?
            parentModel.insertModel(parentModel.size() - 1, (IModel) child);
        }
    }

    /**
     * Does nothing. Because models only copy events when added to one another,
     * we can't just add child events at this point - we need to wait until that
     * child has had it's children added, and so on. So the parent/child link is
     * made in the {@link ModelBuilder#nodeCompleted} method instead.
     *
     * @param parent
     * @param child
     */
    public void setParent(Object parent, Object child) {
    }

}
