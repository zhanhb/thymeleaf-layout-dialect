package nz.net.ultraq.thymeleaf.models.extensions

import org.thymeleaf.model.IText

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
