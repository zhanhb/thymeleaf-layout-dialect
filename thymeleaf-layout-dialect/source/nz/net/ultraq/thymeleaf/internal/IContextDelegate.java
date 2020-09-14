package nz.net.ultraq.thymeleaf.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import javax.annotation.Nonnull;
import nz.net.ultraq.thymeleaf.context.extensions.IContextExtensions;
import org.thymeleaf.context.IContext;
import org.thymeleaf.dialect.IProcessorDialect;

public class IContextDelegate {

    private static final MethodHandle GET_PREFIX_FOR_DIALECT;

    static {
        MethodHandle handle;
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            handle = filterReturnValue(insertArguments(lookup.findStatic(Class.forName("org.codehaus.groovy.runtime.InvokerHelper"), "invokeMethod", methodType(Object.class, Object.class, String.class, Object.class)).asCollector(Object[].class, 1),
                    1, "getPrefixForDialect"),
                    lookup.findStatic(Class.forName("org.codehaus.groovy.runtime.typehandling.ShortTypeHandling"), "castToString", methodType(String.class, Object.class)));
        } catch (ReflectiveOperationException ex) {
            try {
                handle = lookup.findStatic(IContextExtensions.class, "getPrefixForDialect", methodType(String.class, IContext.class, Class.class));
            } catch (NoSuchMethodException | IllegalAccessException ex1) {
                throw new AssertionError(ex1);
            }
        }
        GET_PREFIX_FOR_DIALECT = handle;
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
