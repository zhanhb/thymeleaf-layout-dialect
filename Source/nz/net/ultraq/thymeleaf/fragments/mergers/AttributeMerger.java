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
package nz.net.ultraq.thymeleaf.fragments.mergers;

import java.util.Objects;
import java.util.stream.Stream;
import static nz.net.ultraq.thymeleaf.LayoutDialect.DIALECT_PREFIX_LAYOUT;
import nz.net.ultraq.thymeleaf.fragments.FragmentMerger;
import static nz.net.ultraq.thymeleaf.fragments.FragmentProcessor.PROCESSOR_NAME_FRAGMENT;
import org.thymeleaf.dom.Attribute;
import org.thymeleaf.dom.Element;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.processor.attr.StandardWithAttrProcessor;

/**
 * Merges a source element's attributes into a target element.
 *
 * @author Emanuel Rabina
 */
public class AttributeMerger implements FragmentMerger {

	
	/**
	 * Returns whether or not an attribute is an attribute processor of the
	 * given name, checks both prefix:processor and data-prefix-processor
	 * variants.
	 *
	 * @param prefix
	 * @param name
	 * @return <tt>true</tt> if this attribute is an attribute processor of the
	 * matching name.
	 */
	private static boolean equalsName(Attribute delegate, String prefix, String name) {
		String originalName = delegate.getOriginalName();
		return Objects.equals(originalName, prefix + ":" + name) || Objects.equals(originalName, "data-" + prefix + "-" + name);
	}

	private static String getAttributeValue(Element delegate, String prefix, String name) {
		String attributeValue = delegate.getAttributeValue(prefix + ":" + name);
		if (attributeValue == null || attributeValue.isEmpty()) {
			attributeValue = delegate.getAttributeValue("data-" + prefix + "-" + name);
		}
		return attributeValue;
	}

	/**
	 * Merge source element attributes into a target element, overwriting those
	 * attributes found in the target with those from the source.
	 *
	 * @param sourceElement
	 * @param targetElement
	 */
	@Override
	public void merge(Element targetElement, Element sourceElement) {

		if (sourceElement == null || targetElement == null) {
			return;
		}

		// Exclude the copying of fragment attributes
		Stream<Attribute> sourceAttributes = sourceElement.getAttributeMap().values().stream().filter(sourceAttribute
				-> !equalsName(sourceAttribute, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_FRAGMENT)
		);
		// Merge th:with attributes
		sourceAttributes.forEach(sourceAttribute -> {
			if (equalsName(sourceAttribute, StandardDialect.PREFIX, StandardWithAttrProcessor.ATTR_NAME)) {
				String mergedWithValue = new VariableDeclarationMerger().merge(sourceAttribute.getValue(),
						getAttributeValue(targetElement, StandardDialect.PREFIX, StandardWithAttrProcessor.ATTR_NAME));
				targetElement.setAttribute("${StandardDialect.PREFIX}:${StandardWithAttrProcessor.ATTR_NAME}", mergedWithValue);
			} // Copy every other attribute straight
			else {
				targetElement.setAttribute(sourceAttribute.getOriginalName(), sourceAttribute.getValue());
			}
		});
	}

}
