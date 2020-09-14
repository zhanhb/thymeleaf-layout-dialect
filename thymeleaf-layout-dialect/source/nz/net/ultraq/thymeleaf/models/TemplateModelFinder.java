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
package nz.net.ultraq.thymeleaf.models;

import java.util.Collections;
import java.util.Objects;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import nz.net.ultraq.thymeleaf.internal.IContextDelegate;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.util.StringUtils;

/**
 * A simple API for retrieving (immutable template) models using Thymeleaf's
 * template manager.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
public class TemplateModelFinder {

    private final ITemplateContext context;

    /**
     * Constructor, set the template context we're working in.
     *
     * @param context
     */
    public TemplateModelFinder(ITemplateContext context) {
        this.context = context;
    }

    /**
     * Return a model for any arbitrary item in a template.
     *
     * @param templateName
     * @param selector A Thymeleaf DOM selector, which in turn is an AttoParser
     * DOM selector. See the Appendix in the Using Thymeleaf docs for the DOM
     * selector syntax.
     * @return Model for the selected template and selector.
     */
    private TemplateModel find(String templateName, String selector) {
        return context.getConfiguration().getTemplateManager().parseStandalone(context,
                templateName, StringUtils.isEmpty(selector) ? null : Collections.singleton(selector), context.getTemplateMode(), true, true);
    }

    private TemplateModel find(String templateName) {
        return find(templateName, null);
    }

    /**
     * Return the model specified by the given fragment expression.
     *
     * @param fragmentExpression
     * @return Fragment matching the fragment specification.
     */
    public TemplateModel findFragment(FragmentExpression fragmentExpression) {
        String templateName = "this";
        String dialectPrefix = IContextDelegate.getPrefixForDialect(context, LayoutDialect.class);
        IStandardExpression expression = fragmentExpression.getTemplateName();
        if (expression != null) {
            Object result = expression.execute(context);
            if (result != null) {
                templateName = String.valueOf(result);
            }
        }
        if (Objects.equals(templateName, "this")) {
            templateName = context.getTemplateData().getTemplate();
        }
        IStandardExpression fragmentSelector = fragmentExpression.getFragmentSelector();
        Object execute = null;
        if (fragmentSelector != null) {
            execute = fragmentSelector.execute(context);
        }
        return findFragment(templateName, execute != null ? execute.toString() : null, dialectPrefix);
    }

    /**
     * Return the model specified by the given fragment expression.
     *
     * @param templateName
     * @param fragmentName
     * @param dialectPrefix
     * @return Fragment matching the fragment specification.
     */
    public TemplateModel findFragment(String templateName, String fragmentName, String dialectPrefix) {
        return find(templateName,
                // Attoparser fragment selector, picks a fragment with layout:fragment="name"
                // or starts with layout:fragment="name( or layout:fragment="name ( plus
                // their data attribute equivalents. See the attoparser API docs for details:
                // http://www.attoparser.org/apidocs/attoparser/2.0.0.RELEASE/org/attoparser/select/package-summary.html
                !StringUtils.isEmpty(templateName) && !StringUtils.isEmpty(fragmentName) ? "//["
                + dialectPrefix + ":fragment='" + fragmentName + "' or "
                + dialectPrefix + ":fragment^='" + fragmentName + "(' or "
                + dialectPrefix + ":fragment^='" + fragmentName + " (' or "
                + "data-" + dialectPrefix + "-fragment='" + fragmentName + "' or "
                + "data-" + dialectPrefix + "-fragment^='" + fragmentName + "(' or "
                + "data-" + dialectPrefix + "-fragment^='" + fragmentName + " ('"
                + "]" : null
        );
    }

    /**
     * Return the model specified by the given fragment expression.
     *
     * @param templateName
     * @param fragmentName
     * @return Fragment matching the fragment specification.
     */
    public TemplateModel findFragment(String templateName, String fragmentName) {
        return findFragment(templateName, fragmentName, null);
    }

    /**
     * Return the model specified by the given fragment expression.
     *
     * @param templateName
     * @return Fragment matching the fragment specification.
     */
    public TemplateModel findFragment(String templateName) {
        return findFragment(templateName, null, null);
    }

    /**
     * Return a model for the template specified by the given fragment
     * expression.
     *
     * @param fragmentExpression
     * @return Template model matching the fragment specification.
     */
    public TemplateModel findTemplate(FragmentExpression fragmentExpression) {
        return find(String.valueOf(fragmentExpression.getTemplateName().execute(context)));
    }

    /**
     * Return a model for the template specified by the given template name.
     *
     * @param templateName
     * @return Template model matching the fragment specification.
     */
    public TemplateModel findTemplate(String templateName) {
        return find(templateName);
    }

    public final ITemplateContext getContext() {
        return this.context;
    }

}
