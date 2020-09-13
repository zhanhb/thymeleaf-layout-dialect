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

package nz.net.ultraq.thymeleaf.context.extensions

import nz.net.ultraq.thymeleaf.internal.Extensions as Z;
import org.thymeleaf.context.IContext
import org.thymeleaf.dialect.IProcessorDialect

/**
 * Meta-programming extensions to the {@link IContext} class.
 *
 * @author zhanhb
 * @author Emanuel Rabina
 */
class IContextExtensions {

	private static final String DIALECT_PREFIX_PREFIX = 'DialectPrefix::'

	/**
	 * Apply extensions to the {@code IContext} class.
	 */
	static void apply() {

		IContext.metaClass {
			getAt << { String name ->
				delegate.getVariable(name)
			}
			getOrCreate << { String key, Closure closure ->
                Z.getOrCreate(delegate, key, closure)
			}
			getPrefixForDialect << { Class<IProcessorDialect> dialectClass ->
                Z.getPrefixForDialect(delegate, dialectClass)
			}
			putAt << { String name, Object value ->
				delegate.setVariable(name, value)
			}
		}
	}
}
