package nz.net.ultraq.thymeleaf.models.extensions

import org.thymeleaf.model.ICloseElementTag

class ICloseElementTagExtensions {
	static void apply() {
        ICloseElementTag.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
        }
	}
}
