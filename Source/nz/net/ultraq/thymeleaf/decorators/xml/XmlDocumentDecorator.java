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
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import nz.net.ultraq.thymeleaf.models.AttributeMerger;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;

/**
 * A decorator made to work over an XML document.
 *
 * @author Emanuel Rabina
 */
public class XmlDocumentDecorator implements Decorator {

    protected final IModelFactory modelFactory;

    /**
     * Constructor, set up the document decorator context.
     *
     * @param modelFactory
     */
    public XmlDocumentDecorator(IModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    /**
     * Decorates the target XML document with the source one.
     *
     * @param targetDocumentModel
     * @param sourceDocumentModel
     */
    @Override
    public void decorate(IModel targetDocumentModel, IModel sourceDocumentModel) {

        // TODO
        // Copy text outside of the root element, keeping whitespace copied to a minimum
        //boolean beforeHtml = true;
        //boolean allowNext = false;
        //Node lastNode = contentXml;
        //for (Node externalNode : decoratorDocument.getChildren()) {
        //    if (externalNode == decoratorXml) {
        //        beforeHtml = false;
        //        allowNext = true;
        //    } else if (externalNode instanceof Comment || allowNext) {
        //        if (beforeHtml) {
        //            contentDocument.insertBefore(contentXml, externalNode);
        //        } else {
        //            contentDocument.insertAfter(lastNode, externalNode);
        //            lastNode = externalNode;
        //        }
        //        allowNext = externalNode instanceof Comment;
        //    }
        //}
        // Find the root element of the target document to merge
        IModel targetDocumentRootModel = MetaClass.findModel(targetDocumentModel, targetDocumentEvent -> {
            return targetDocumentEvent instanceof IOpenElementTag;
        });

        // Decorate the target document with the source one (which is the one being processed)
        new AttributeMerger(modelFactory).merge(targetDocumentRootModel, sourceDocumentModel);
        MetaClass.replaceModel(targetDocumentModel, targetDocumentRootModel);
    }

}
