package nz.net.ultraq.thymeleaf.models.extensions

import nz.net.ultraq.thymeleaf.internal.Extensions as Z;
import org.thymeleaf.model.IText;

/**
 *
 * @author zhanhb
 */
class ITextExtensions {
	static void apply() {
        IText.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
            isWhitespace << {
                Z.isWhitespace(delegate)
            }
        }
	}
}
