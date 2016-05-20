package nz.net.ultraq.thymeleaf.decorators.strategies;

import java.util.Arrays;
import java.util.function.Predicate;
import org.thymeleaf.dom.Comment;
import org.thymeleaf.dom.Element;
import org.thymeleaf.dom.Node;

/**
 * Enum for the types of elements in the HEAD section that we might need to
 * sort.
 *
 * TODO: Expand this to include more element types as they are requested.
 */
enum HeadNodeTypes {

	COMMENT(node -> node instanceof Comment),
	META(node -> node instanceof Element && "meta".equals(((Element) node).getNormalizedName())),
	STYLESHEET(node -> node instanceof Element && "link".equals(((Element) node).getNormalizedName()) && "stylesheet".equals(((Element) node).getAttributeValue("rel"))),
	SCRIPT(node -> node instanceof Element && "script".equals(((Element) node).getNormalizedName())),
	OTHER_ELEMENT(node -> node instanceof Element);

	final Predicate<Node> determinant;

	/**
	 * Constructor, set the test that matches this type of head node.
	 *
	 * @param determinant
	 */
	HeadNodeTypes(Predicate<Node> determinant) {
		this.determinant = determinant;
	}

	/**
	 * Figure out the enum for the given node type.
	 *
	 * @param element The node to match.
	 * @return Matching <tt>HeadNodeTypes</tt> enum to descript the node.
	 */
	static HeadNodeTypes findMatchingType(Node element) {
		return Arrays.stream(values()).filter(headNodeType
				-> headNodeType.determinant.test(element)
		).findFirst().orElse(null);
	}
}
