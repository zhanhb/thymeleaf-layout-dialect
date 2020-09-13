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
package nz.net.ultraq.thymeleaf.fragments;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.thymeleaf.context.IContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.processor.element.IElementModelStructureHandler;

/**
 * Holds the layout fragments encountered across layout/decorator and content
 * templates for use later.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})
public class FragmentMap extends HashMap<String, List<IModel>> {

    private static final String FRAGMENT_COLLECTION_KEY = "LayoutDialect::FragmentCollection";

    /**
     * Retrieves either the fragment map for the current context, or returns a
     * new fragment map.
     *
     * @param context
     * @return A new or existing fragment collection for the context.
     */
    public static Map<String, List<IModel>> get(IContext context) {
        @SuppressWarnings("unchecked")
        Map<String, List<IModel>> localVariable = (Map<String, List<IModel>>) context.getVariable(FRAGMENT_COLLECTION_KEY);
        return localVariable != null ? localVariable : new FragmentMap();
    }

    /**
     * Set the fragment collection to contain whatever it initially had, plus
     * the given fragments, just for the scope of the current node.
     *
     * @param context
     * @param structureHandler
     * @param fragments The new fragments to add to the map.
     */
    public static void setForNode(IContext context, IElementModelStructureHandler structureHandler,
            @Nonnull Map<String, List<IModel>> fragments) {
        Map<String, List<IModel>> accumulator = get(context);
        LinkedHashMap<String, List<IModel>> clone = new LinkedHashMap<>(fragments);
        for (Entry<String, List<IModel>> entry : accumulator.entrySet()) {
            String fragmentName = entry.getKey();
            List<IModel> fragmentList = entry.getValue();
            List<IModel> list = clone.get(fragmentName);
            if (list != null) {
                list.addAll(fragmentList);
            } else {
                clone.put(fragmentName, fragmentList);
            }
        }
        structureHandler.setLocalVariable(FRAGMENT_COLLECTION_KEY, clone);
    }

}
