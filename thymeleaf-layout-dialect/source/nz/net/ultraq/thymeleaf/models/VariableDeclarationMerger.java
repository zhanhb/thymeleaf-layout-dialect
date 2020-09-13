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
package nz.net.ultraq.thymeleaf.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.util.StringUtils;

/**
 * Merges variable declarations in a {@code th:with} attribute processor, taking
 * the declarations in the target and combining them with the declarations in
 * the source, overriding any same-named declarations in the target.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class VariableDeclarationMerger {

    private final IExpressionContext context;

    /**
     * Constructor, sets the processing context for the merger.
     *
     * @param context
     */
    public VariableDeclarationMerger(IExpressionContext context) {
        this.context = context;
    }

    /**
     * Merge {@code th:with} attributes so that names from the source value
     * overwrite the same names in the target value.
     *
     * @param target
     * @param source
     * @return
     */
    public String merge(String target, String source) {
        if (StringUtils.isEmpty(target)) {
            return source;
        }
        if (StringUtils.isEmpty(source)) {
            return target;
        }
        VariableDeclarationParser declarationParser = new VariableDeclarationParser(context);
        List<VariableDeclaration> targetDeclarations = declarationParser.parse(target);
        List<VariableDeclaration> sourceDeclarations = declarationParser.parse(source);

        List<VariableDeclaration> newDeclarations = new ArrayList<>(targetDeclarations.size() + sourceDeclarations.size());
        for (VariableDeclaration targetDeclaration : targetDeclarations) {
            VariableDeclaration override = null;
            String name = targetDeclaration.getName();
            for (VariableDeclaration sourceDeclaration : sourceDeclarations) {
                if (Objects.equals(name, sourceDeclaration.getName())) {
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

        newDeclarations.addAll(sourceDeclarations);

        StringBuilder buffer = new StringBuilder(source.length() + target.length());
        boolean first = true;

        for (Object value : newDeclarations) {
            if (first) {
                first = false;
            } else {
                buffer.append(',');
            }
            buffer.append(value);
        }
        return buffer.toString();
    }

    public final IExpressionContext getContext() {
        return this.context;
    }

}
