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

import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.ExpressionContext
import org.thymeleaf.standard.expression.FragmentExpression
import org.thymeleaf.standard.expression.VariableExpression

/**
 * Tests for the expression processor module.
 * 
 * @author Emanuel Rabina
 */
class ExpressionProcessorTests {

	private static TemplateEngine templateEngine
	private ExpressionContext expressionContext
	private ExpressionProcessor expressionProcessor

	/**
	 * Setup, create the template engine.
	 */
	@BeforeClass
	static void setupTemplateEngine() {

		templateEngine = new TemplateEngine()
	}

	/**
	 * Test setup, a new expression processor.
	 */
	@Before
	void setupExpressionProcessor() {

		expressionContext = new ExpressionContext(templateEngine.configuration)
		expressionProcessor = new ExpressionProcessor(expressionContext)
	}

	/**
	 * Test that we get a Thymeleaf expression out of the expression processor.
	 */
	@Test
	void parse() {

		def expression = expressionProcessor.parse('${1 + 1}')
		assert expression instanceof VariableExpression
	}

	/**
	 * Test backwards compatibility wrapping for older Thymeleaf 2 expressions.
	 */
	@Test
	void parseFragmentExpression() {

		def fragmentExpression

		fragmentExpression = expressionProcessor.parseFragmentExpression('~{hello.html}')
		assert fragmentExpression instanceof FragmentExpression
		assert fragmentExpression.templateName.execute(expressionContext) == 'hello.html'

		// Backwards compatibility test
		fragmentExpression = expressionProcessor.parseFragmentExpression('hello.html')
		assert fragmentExpression instanceof FragmentExpression
		assert fragmentExpression.templateName.execute(expressionContext) == 'hello.html'
	}

	/**
	 * {@code null} testing of fragment expression parsing since it does
	 * operations on the expression before passing it to the parser.
	 */
	@Test
	void parseFragmentExpressionNull() {

		try {
			expressionProcessor.parseFragmentExpression(null)
			assert false
		}
		catch (ex) {
			assert ex instanceof IllegalArgumentException
		}
	}

	/**
	 * Tests multi-line fragment expressions.
	 */
	@Test
	void parseFragmentExpressionMultiline() {

		def fragmentExpression

		fragmentExpression = expressionProcessor.parseFragmentExpression('''~{hello::fragment(
			'blah',
			1)
			}''')
		assert fragmentExpression instanceof FragmentExpression
		assert fragmentExpression.templateName.execute(expressionContext) == 'hello'

		// Backwards compatibility test
		fragmentExpression = expressionProcessor.parseFragmentExpression('''hello::fragment(
			'blah',
			1)''')
		assert fragmentExpression instanceof FragmentExpression
		assert fragmentExpression.templateName.execute(expressionContext) == 'hello'
	}

	/**
	 * Process the expression this time to get a result.
	 */
	@Test
	void process() {

		def result = expressionProcessor.process('${1 + 1}')
		assert result == 2
	}

	/**
	 * Process the expression but return as a string result.
	 */
	@Test
	void processAsString() {

		def resultAsString = expressionProcessor.processAsString('${1 + 1}')
		assert resultAsString == '2'
	}
}
