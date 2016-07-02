package nz.net.ultraq.thymeleaf.models.extensions

import org.thymeleaf.model.IStandaloneElementTag

class IStandaloneElementTagExtensions {
	static void apply() {
        IStandaloneElementTag.metaClass {
            equals << { Object other ->
                Z.equals(delegate, other)
            }
        }
	}
}
