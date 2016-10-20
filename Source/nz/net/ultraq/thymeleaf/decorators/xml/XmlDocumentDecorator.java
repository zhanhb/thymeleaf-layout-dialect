/*
 * Copyright 2013, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf.decorators.xml;

import nz.net.ultraq.thymeleaf.decorators.Decorator;
import nz.net.ultraq.thymeleaf.internal.Extensions;
import nz.net.ultraq.thymeleaf.models.AttributeMerger;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IDocType;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;

/**
 * A decorator made to work over an XML document.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class XmlDocumentDecorator implements Decorator {

    // Find the root element of each document to work with
    private static IModel rootModelFinder(IModel documentModel) {
        return Extensions.findModel(documentModel, documentEvent -> {
            return documentEvent instanceof IOpenElementTag;
        });
    }

    private static boolean documentContainsDocType(IModel document) {
        for (int i = 0, size = document.size(); i < size; i++) {
            ITemplateEvent event = document.get(i);
            if (event instanceof IDocType) {
                return true;
            }
            if (event instanceof IOpenElementTag) {
                break;
            }
        }
        return false;
    }

    protected final ITemplateContext context;

    /**
     * Constructor, set up the document decorator context.
     *
     * @param context
     */
    public XmlDocumentDecorator(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Decorates the target XML document with the source one.
     *
     * @param targetDocumentModel
     * @param sourceDocumentModel
     * @return Result of the decoration.
     */
    @Override
    public IModel decorate(IModel targetDocumentModel, IModel sourceDocumentModel) {
        IModelFactory modelFactory = context.getModelFactory();

        IModel targetDocumentRootModel = rootModelFinder(targetDocumentModel);
        IModel sourceDocumentRootModel = rootModelFinder(sourceDocumentModel);

        // Decorate the target document with the source one
        IModel resultDocumentModel = new AttributeMerger(context).merge(targetDocumentRootModel, sourceDocumentRootModel);

        // Copy comments outside of the root element, keeping whitespace copied to a minimum
        final int size = targetDocumentModel.size();
        for (int i = 0; i < size; i++) {
            ITemplateEvent event = targetDocumentModel.get(i);
            // Only copy doctypes if the source document doesn't already have one
            if (event instanceof IDocType) {
                if (!documentContainsDocType(sourceDocumentModel)) {
                    Extensions.insertWithWhitespace(resultDocumentModel, 0, event, modelFactory);
                }
            } else if (event instanceof IComment) {
                Extensions.insertWithWhitespace(resultDocumentModel, 0, event, modelFactory);
            } else if (event instanceof IOpenElementTag) {
                break;
            }
        }
        for (int i = size - 1; i >= 0; i--) {
            ITemplateEvent event = targetDocumentModel.get(i);
            if (event instanceof IComment) {
                Extensions.insertWithWhitespace(resultDocumentModel, resultDocumentModel.size(), event, modelFactory);
            } else if (event instanceof ICloseElementTag) {
                break;
            }
        }

        return resultDocumentModel;
    }

}
