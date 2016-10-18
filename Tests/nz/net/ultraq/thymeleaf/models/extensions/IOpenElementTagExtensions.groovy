package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.Extensions as Z;
import org.thymeleaf.model.IOpenElementTag

/**
 *
 * @author zhanhb
 */
class IOpenElementTagExtensions {
	static void apply() {
        IOpenElementTag.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
        }
	}
}
