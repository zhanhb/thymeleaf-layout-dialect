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
package nz.net.ultraq.thymeleaf.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

/**
 * Extracts just the parameter names from a fragment definition. Used for when
 * unnamed fragment parameters need to be mapped to their respective names.
 *
 * @author Emanuel Rabina
 */
public class FragmentParameterNamesExtractor {

    private static final Pattern FRAGMENT_WITH_PARAMETERS_PATTERN = Pattern.compile(".*?\\(.*\\)");

    /**
     * Returns a list of parameter names for the given fragment definition.
     *
     * @param fragmentDefinition
     * @return A list of the named parameters, in the order they are defined.
     */
    @Nonnull
    public List<String> extract(@Nonnull String fragmentDefinition) {

        List<String> parameterNames;
        if (FRAGMENT_WITH_PARAMETERS_PATTERN.matcher(fragmentDefinition).matches()) {
            String parametersDefinition = fragmentDefinition.substring(
                    fragmentDefinition.indexOf('(') + 1, fragmentDefinition.lastIndexOf(')'));
            String[] definitions = parametersDefinition.split(",");
            parameterNames = new ArrayList<>(definitions.length);
            for (String parameter : definitions) {
                parameterNames.add(parameter.contains("=") ? parameter.substring(0, parameter.indexOf('=')).trim() : parameter.trim());
            }
        } else {
            parameterNames = new ArrayList<>(0);
        }
        return parameterNames;
    }

}
