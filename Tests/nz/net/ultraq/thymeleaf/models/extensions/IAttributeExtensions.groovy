package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.MetaClass as Z;
import org.thymeleaf.model.IAttribute;

/**
 *
 * @author zhanhb
 */
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
