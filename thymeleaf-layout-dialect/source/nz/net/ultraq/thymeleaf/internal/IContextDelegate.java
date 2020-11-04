package nz.net.ultraq.thymeleaf.internal;

import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.*;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodType.methodType;
import javax.annotation.Nonnull;
import nz.net.ultraq.thymeleaf.context.extensions.IContextExtensions;
import org.thymeleaf.context.IContext;
import org.thymeleaf.dialect.IProcessorDialect;

public class IContextDelegate {

    private static final MethodHandle GET_PREFIX_FOR_DIALECT = init();

    @SuppressWarnings({"UseSpecificCatch", "BroadCatchBlock", "TooBroadCatch"})
    private static MethodHandle init() {
        Lookup lookup = lookup();
        try {
            try {
                return filterReturnValue(insertArguments(lookup.findStatic(Class.forName("org.codehaus.groovy.runtime.InvokerHelper"), "invokeMethod", methodType(Object.class, Object.class, String.class, Object.class)).asCollector(Object[].class, 1),
                        1, "getPrefixForDialect"),
                        lookup.findStatic(Class.forName("org.codehaus.groovy.runtime.typehandling.ShortTypeHandling"), "castToString", methodType(String.class, Object.class)));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return lookup.findStatic(IContextExtensions.class, "getPrefixForDialect", methodType(String.class, IContext.class, Class.class));
            }
        } catch (Throwable ex) {
            return dropArguments(throwException(String.class, Throwable.class).bindTo(ex), 0, Object.class, Class.class);
        }
    }

    public static String getPrefixForDialect(@Nonnull IContext self, Class<? extends IProcessorDialect> dialectClass) {
        try {
            return (String) GET_PREFIX_FOR_DIALECT.invoke(self, dialectClass);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new java.lang.reflect.UndeclaredThrowableException(ex);
        }
    }

}
