package nz.net.ultraq.thymeleaf.models.extensions

import org.thymeleaf.model.IAttribute;

class IAttributeExtensions {
	static void apply() {
        IAttribute.metaClass {
            equalsName << { String prefix, String name ->
                Z.equalsName(delegate, prefix, name)
            }
            getAttributeName << {
                Z.getAttributeName(delegate)
            }
        }
	}
}
