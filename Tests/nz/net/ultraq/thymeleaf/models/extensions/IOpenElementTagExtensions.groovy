package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.MetaClass as Z;
import org.thymeleaf.model.IOpenElementTag

class IOpenElementTagExtensions {
	static void apply() {
        IOpenElementTag.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
        }
	}
}
