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
package nz.net.ultraq.thymeleaf.fragments.mergers;

import java.util.ArrayList;
import java.util.List;
import org.thymeleaf.util.StringUtils;

/**
 * Merges variable declarations in a <tt>th:with</tt> attribute processor,
 * taking the declarations in the source and overriding same-named declarations
 * in the target.
 *
 * @author Emanuel Rabina
 */
public class VariableDeclarationMerger {

    /**
     * Create variable declaration objects out of the declaration string.
     *
     * @param declarationString
     * @return A list of variable declaration objects that make up the
     * declaration string.
     */
    private static List<VariableDeclaration> deriveDeclarations(String declarationString) {
        String[] attributeTokens = declarationString.split(",");
        ArrayList<VariableDeclaration> arrayList = new ArrayList<VariableDeclaration>(attributeTokens.length);
        for (String attributeToken : attributeTokens) {
            arrayList.add(new VariableDeclaration(attributeToken));
        }
        return arrayList;
    }

    /**
     * Merge <tt>th:with</tt> attributes so that names from the source value
     * overwrite the same names in the target value.
     */
    public String merge(String target, String source) {
        if (StringUtils.isEmpty(target)) {
            return source;
        }
        if (StringUtils.isEmpty(source)) {
            return target;
        }
        List<VariableDeclaration> targetDeclarations = deriveDeclarations(target);
        List<VariableDeclaration> sourceDeclarations = deriveDeclarations(source);

        List<VariableDeclaration> newDeclarations = new ArrayList<VariableDeclaration>(targetDeclarations.size() + sourceDeclarations.size());
        for (VariableDeclaration targetDeclaration : targetDeclarations) {
            VariableDeclaration override = null;
            for (VariableDeclaration sourceDeclaration : sourceDeclarations) {
                if (sourceDeclaration.getName().equals(targetDeclaration.getName())) {
                    override = sourceDeclaration;
                    break;
                }
            }
            if (override != null) {
                sourceDeclarations.remove(override);
                newDeclarations.add(override);
            } else {
                newDeclarations.add(targetDeclaration);
            }
        }
        for (VariableDeclaration targetAttributeDeclaration : sourceDeclarations) {
            newDeclarations.add(targetAttributeDeclaration);
        }
        if (newDeclarations.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder().append(newDeclarations.get(0));
        for (int i = 1, size = newDeclarations.size(); i < size; i++) {
            sb.append(',').append(newDeclarations.get(i));
        }
        return sb.toString();
    }

}
