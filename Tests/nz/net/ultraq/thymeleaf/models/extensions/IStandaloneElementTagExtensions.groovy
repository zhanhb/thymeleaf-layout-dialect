package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.MetaClass as Z;
import org.thymeleaf.model.IStandaloneElementTag

/**
 *
 * @author zhanhb
 */
class IStandaloneElementTagExtensions {
	static void apply() {
        IStandaloneElementTag.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
        }
	}
}
