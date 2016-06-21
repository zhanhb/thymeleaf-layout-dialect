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
package nz.net.ultraq.thymeleaf.expressions;

import java.util.regex.Pattern;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;

/**
 * A simplified API for working with Thymeleaf expressions.
 *
 * @author Emanuel Rabina
 */
public class ExpressionProcessor {

    private static final Pattern THYMELEAF_3_FRAGMENT_EXPRESSION = Pattern.compile("^~\\{.+\\}$");

    private final ITemplateContext context;

    /**
     * Constructor, sets the execution context.
     *
     * @param context
     */
    public ExpressionProcessor(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Parses an expression, returning the matching expression type.
     *
     * @param expression
     * @return Matching expression type.
     */
    public IStandardExpression parse(String expression) {
        return StandardExpressions.getExpressionParser(context.getConfiguration())
                .parseExpression(context, expression);
    }

    /**
     * Parses an expression under the assumption it is a fragment expression.
     * This method will take care to wrap fragment expressions written in
     * Thymeleaf 2 syntax as a backwards compatibility measure for those
     * migrating their wep apps to Thymeleaf 3.
     *
     * @param expression
     * @return A fragment expression.
     */
    public FragmentExpression parseFragmentExpression(String expression) {
        return (FragmentExpression) parse(THYMELEAF_3_FRAGMENT_EXPRESSION.matcher(expression).matches() ? expression : "~{" + expression + "}");
    }

    /**
     * Parses and executes an expression, returning the result of the expression
     * having been parsed and executed.
     *
     * @param expression
     * @return The result of the expression being executed.
     */
    public Object process(String expression) {
        return parse(expression).execute(context);
    }

    /**
     * Parse and execute an expression, returning the result as a string. Useful
     * for expressions that expect a simple result.
     *
     * @param expression
     * @return The expression as a string.
     */
    public String processAsString(String expression) {
        return String.valueOf(process(expression));
    }

}
