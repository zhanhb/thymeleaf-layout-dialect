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

package nz.net.ultraq.thymeleaf.expressions.tests

import nz.net.ultraq.thymeleaf.expressions.ExpressionProcessor

import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.ExpressionContext
import org.thymeleaf.standard.expression.FragmentExpression
import org.thymeleaf.standard.expression.VariableExpression
import spock.lang.Specification

/**
 * Tests for the expression processor module.
 * 
 * @author Emanuel Rabina
 */
@SuppressWarnings('GStringExpressionWithinString')
class ExpressionProcessorTests extends Specification {

	private final TemplateEngine templateEngine = new TemplateEngine()
	private final ExpressionContext expressionContext = new ExpressionContext(templateEngine.configuration)
	private final ExpressionProcessor expressionProcessor = new ExpressionProcessor(expressionContext)

	def "Get a Thymeleaf expression out of the expression processor"() {
		expect:
			expressionProcessor.parse('${1 + 1}') instanceof VariableExpression
	}

	def "Returns a fragment expression"() {
		when:
			def fragmentExpression = expressionProcessor.parseFragmentExpression('~{hello.html}')
		then:
			fragmentExpression instanceof FragmentExpression
			fragmentExpression.templateName.execute(expressionContext) == 'hello.html'
	}

	def "Returns a fragment expression - backwards compatibility wrapping for Thymeleaf 2"() {
		when:
			def fragmentExpression = expressionProcessor.parseFragmentExpression('hello.html')
		then:
			fragmentExpression instanceof FragmentExpression
			fragmentExpression.templateName.execute(expressionContext) == 'hello.html'
	}

	def "null testing of fragment expression parsing"() {
		when:
			expressionProcessor.parseFragmentExpression(null)
		then:
			thrown(IllegalArgumentException)
	}

	def "Multi-line fragment expressions"() {
		when:
			def fragmentExpression = expressionProcessor.parseFragmentExpression('''~{hello::fragment(
				'blah',
				1)
				}''')
		then:
			fragmentExpression instanceof FragmentExpression
			fragmentExpression.templateName.execute(expressionContext) == 'hello'
	}

	def "Multi-line fragment expressions - backwards compatibility wrapping for Thymeleaf 2"() {
		when:
			def fragmentExpression = expressionProcessor.parseFragmentExpression('''hello::fragment(
				'blah',
				1)''')
		then:
			fragmentExpression instanceof FragmentExpression
			fragmentExpression.templateName.execute(expressionContext) == 'hello'
	}

	def "Processing an expression returns a usable result"() {
		expect:
			expressionProcessor.process('${1 + 1}') == 2
	}

	def "Processing an expression returning a string result"() {
		expect:
			expressionProcessor.processAsString('${1 + 1}') == '2'
	}
}
