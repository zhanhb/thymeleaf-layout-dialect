package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.MetaClass as Z;
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
