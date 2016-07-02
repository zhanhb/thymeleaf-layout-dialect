package nz.net.ultraq.thymeleaf.models.extensions

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
