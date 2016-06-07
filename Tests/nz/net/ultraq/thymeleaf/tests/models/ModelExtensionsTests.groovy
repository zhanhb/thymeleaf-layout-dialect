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

package nz.net.ultraq.thymeleaf.tests.models

import nz.net.ultraq.thymeleaf.LayoutDialect
import nz.net.ultraq.thymeleaf.internal.MetaClass
import nz.net.ultraq.thymeleaf.models.ModelBuilder

import org.junit.BeforeClass
import org.junit.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import static org.junit.Assert.*

/**
 * Tests for some of the more complicated additions to the model class.
 * 
 * @author Emanuel Rabina
 */
class ModelExtensionsTests {

	private static ModelBuilder modelBuilder

	/**
	 * Set up, create a template engine.
	 */
	@BeforeClass
	static void setupThymeleafEngine() {

		def templateEngine = new TemplateEngine(
			additionalDialects: [
				new LayoutDialect()
			]
		)
		modelBuilder = new ModelBuilder(templateEngine.configuration.getModelFactory(TemplateMode.HTML),
			templateEngine.configuration.elementDefinitions, TemplateMode.HTML)
	}

	/**
	 * Test the child model iterator to retrieve only the immediate children of a
	 * model as their own models.
	 */
	@Test
	void childModelIterator() {

		def pModel1 = modelBuilder.build {
			p('Test paragraph')
		}
		def hrModel = modelBuilder.build {
			hr(standalone: true)
		}
		def pModel2 = modelBuilder.build {
			p('Another test paragraph')
		}

		def model = modelBuilder.build {
			div(class: 'content') {
				add(pModel1)
				add(hrModel)
				add(pModel2)
			}
		}

		def childModelIterator = MetaClass.childModelIterator(model)

		def nextModel = childModelIterator.next()
		assertTrue(MetaClass.equalsIgnoreWhitespace(nextModel, pModel1))
		assertEquals(1, MetaClass.getMetaClass(nextModel).startIndex)
		assertEquals(4, MetaClass.getMetaClass(nextModel).endIndex)

		nextModel = childModelIterator.next()
		assertTrue(MetaClass.equalsIgnoreWhitespace(nextModel, hrModel))
		assertEquals(4, MetaClass.getMetaClass(nextModel).startIndex)
		assertEquals(5, MetaClass.getMetaClass(nextModel).endIndex)

		nextModel = childModelIterator.next()
		assertTrue(MetaClass.equalsIgnoreWhitespace(nextModel, pModel2))
		assertEquals(5, MetaClass.getMetaClass(nextModel).startIndex)
		assertEquals(8, MetaClass.getMetaClass(nextModel).endIndex)

		assertFalse(childModelIterator.hasNext())
	}

	/**
	 * Test the retrieval of models containing standard HTML/XML elements.
	 */
	@Test
	void getModel() {

		def model = modelBuilder.build {
			section {
				header {
					h1('Test title')
				}
				div(class: 'content') {
					p('Test paragraph')
					p('Another test paragraph')
				}
			}
		}

		def modelExtract = MetaClass.getModel(model, 0)
		assertTrue(MetaClass.equals(model, modelExtract))
	}

	/**
	 * Tests the retrieval of models containing void elements that are
	 * self-closed, usually to be XML compliant a la XHTML.
	 */
	@Test
	void getModelStandalone() {

		def model = modelBuilder.build {
			div {
				hr(standalone: true)
			}
		}

		def modelExtract = MetaClass.getModel(model, 0)
		assertTrue(MetaClass.equals(model, modelExtract))
	}

	/**
	 * Tests the retrieval of void elements that are neither self-closed or have
	 * a matching closing tag, as per the HTML spec.
	 */
	@Test
	void getModelVoid() {

		def model = modelBuilder.build {
			head {
				meta(charset: 'utf-8', void: true)
			}
		}

		def modelExtract = MetaClass.getModel(model, 0)
		assertTrue(MetaClass.equals(model, modelExtract))
	}

	/**
	 * Tests the retrieval of void elements that have a closing tag, which, isn't
	 * correct HTML as per the spec, but as devs transition from XML-based
	 * Thymeleaf 2 to HTML-based Thymeleaf 3, we may see a lot of as seen here:
	 * https://github.com/ultraq/thymeleaf-layout-dialect/issues/110
	 */
	@Test
	void getModelVoidClosed() {

		def model = modelBuilder.build {
			head {
				meta(charset: 'utf-8')
			}
		}

		def modelExtract = MetaClass.getModel(model, 0)
		assertTrue(MetaClass.equals(model, modelExtract))
	}
}
