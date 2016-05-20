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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import static nz.net.ultraq.thymeleaf.fragments.FragmentProcessor.PROCESSOR_NAME_FRAGMENT;
import static nz.net.ultraq.thymeleaf.includes.IncludeProcessor.PROCESSOR_NAME_INCLUDE;
import static nz.net.ultraq.thymeleaf.includes.ReplaceProcessor.PROCESSOR_NAME_REPLACE;
import nz.net.ultraq.thymeleaf.internal.MetaClass;
import org.thymeleaf.dom.Element;

/**
 * Searches for and returns layout dialect fragments amongst a given set of
 * elements.
 *
 * @author Emanuel Rabina
 */
public class FragmentMapper {

	public static final String DIALECT_PREFIX_LAYOUT = "layout";

	/**
	 * Find and return clones of all fragments within the given elements,
	 * without delving into <tt>layout:include</tt> or <tt>layout:replace</tt>
	 * elements, mapped by the name of each fragment.
	 *
	 * @param elements List of elements to search.
	 * @return Map of fragment names and their elements.
	 */
	public Map<String, Element> map(List<Element> elements) {
		Map<String, Element> fragments = new LinkedHashMap<String, Element>();
		Consumer<Element>[] findFragments = new Consumer[1];
		findFragments[0] = element -> {
			String fragmentName = MetaClass.getAttributeValue(element, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_FRAGMENT);
			if (fragmentName != null && !fragmentName.isEmpty()) {
				Element fragment = (Element) element.cloneNode(null, false);
				MetaClass.removeAttribute(fragment, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_FRAGMENT);
				fragments.put(fragmentName, fragment);
			} else if (!MetaClass.hasAttribute(element, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_INCLUDE)
					|| !MetaClass.hasAttribute(element, DIALECT_PREFIX_LAYOUT, PROCESSOR_NAME_REPLACE)) {
				element.getElementChildren().stream().forEach(findFragments[0]);
			}
		};
		elements.forEach(findFragments[0]);
		return fragments;
	}
}
