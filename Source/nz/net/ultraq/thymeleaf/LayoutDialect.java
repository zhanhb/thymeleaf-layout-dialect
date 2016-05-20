/*
 * Copyright 2012, Emanuel Rabina (http://www.ultraq.net.nz/)
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
package nz.net.ultraq.thymeleaf;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import nz.net.ultraq.thymeleaf.decorators.DecoratorProcessor;
import nz.net.ultraq.thymeleaf.decorators.SortingStrategy;
import nz.net.ultraq.thymeleaf.decorators.TitlePatternProcessor;
import nz.net.ultraq.thymeleaf.decorators.strategies.AppendingStrategy;
import nz.net.ultraq.thymeleaf.fragments.FragmentProcessor;
import nz.net.ultraq.thymeleaf.includes.IncludeProcessor;
import nz.net.ultraq.thymeleaf.includes.ReplaceProcessor;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.processor.IProcessor;

/**
 * Dialect for making use of template/layout decorator pages with Thymeleaf.
 *
 * @author Emanuel Rabina
 */
public class LayoutDialect extends AbstractDialect {

	public static final String DIALECT_NAMESPACE_LAYOUT = "http://www.ultraq.net.nz/thymeleaf/layout";
	public static final String DIALECT_PREFIX_LAYOUT = "layout";

	private final SortingStrategy sortingStrategy;

	final String prefix = DIALECT_PREFIX_LAYOUT;
	final Set<IProcessor> processors;

	/**
	 * Constructor, configure the layout dialect with the given values.
	 *
	 * @param sortingStrategy
	 */
	public LayoutDialect(SortingStrategy sortingStrategy) {
		this.sortingStrategy = sortingStrategy;
		processors = new LinkedHashSet<>(Arrays.asList(
				new DecoratorProcessor(sortingStrategy),
				new IncludeProcessor(),
				new ReplaceProcessor(),
				new FragmentProcessor(),
				new TitlePatternProcessor()
		));
	}

	public LayoutDialect() {
		this(new AppendingStrategy());
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public Set<IProcessor> getProcessors() {
		return Collections.unmodifiableSet(processors);
	}

	public SortingStrategy getSortingStrategy() {
		return sortingStrategy;
	}

}
