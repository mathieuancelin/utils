package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Option;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Mathieu ANCELIN
 */
public class Y {

    public static interface CurryFunction<T> {

        T get();

        <P> CurryFunction<T> _(P arg);
    }

    public static <T> Option<Method> method(Class<T> type, String name, Class<?>... args) {
        Method m = null;
        try {
            m = type.getDeclaredMethod(name, args);
            return Option.some(m);
        } catch (NoSuchMethodException ex) {
            return Option.none();
        }
    }

    public static <T> CurryFunction<T> curry(Option<Method> m, Object on, Class<T> ret) {
        if (m.isDefined()) {
            return curry(m.get(), on, ret);
        } else {
            throw new IllegalStateException("Method unavailable.");
        }
    }

    public static <T> CurryFunction<T> curry(Option<Method> m, Object on, Class<T> ret, Object... with) {
        if (m.isDefined()) {
            return curry(m.get(), on, ret, with);
        } else {
            throw new IllegalStateException("Method unavailable.");
        }
    }

    public static <T> CurryFunction<T> curry(Method m, Object on, Class<T> ret) {
        return new AutoCurryFunctionImpl<T>(m, on);
    }

    public static <T> CurryFunction<T> curry(Method m, Object on, Class<T> ret, Object... with) {
        return new AutoCurryFunctionImpl<T>(m, on, with);
    }

    public static abstract class AbstractCurryFunction<T> implements CurryFunction<T> {

        protected final List<Object> args = new ArrayList<Object>();

        public void init(Object... initArgs) {
            if (initArgs != null && initArgs.length > 0) {
                args.addAll(Arrays.asList(initArgs));
            }
        }

        public int getFilledArgsCount() {
            return args.size();
        }

        public abstract CurryFunction<T> create(List<Object> args);

        @Override
        public <P> CurryFunction<T> _(P arg) {
            List<Object> n1 = new ArrayList<Object>();
            n1.addAll(args);
            n1.add(arg);
            return create(n1);
        }
    }

    private static class AutoCurryFunctionImpl<T> extends AbstractCurryFunction<T> {

        private final Method m;
        private final Object on;

        public AutoCurryFunctionImpl(Method m, Object on, Object... initArgs) {
            this.m = m;
            this.on = on;
            init(initArgs);
        }

        @Override
        public T get() {
            try {
                m.setAccessible(true);
                return (T) m.invoke(on, args.toArray(new Object[args.size()]));
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Error while running curryfied method", ex);
            } catch (IllegalArgumentException ex) {
                return tryArgsRemapping();
            } catch (InvocationTargetException ex) {
                throw new IllegalStateException("Error while running curryfied method", ex);
            }
        }

        private T tryArgsRemapping() {
            Map<Class<?>, List<Object>> foundArgs = new HashMap<Class<?>, List<Object>>();
            for (Object arg : args) {
                if (!foundArgs.containsKey(arg.getClass())) {
                    foundArgs.put(arg.getClass(), new ArrayList<Object>());
                }
                foundArgs.get(arg.getClass()).add(arg);
            }
            Object[] finalArgs = new Object[args.size()];
            int i = 0;
            for (Class<?> expectedType : m.getParameterTypes()) {
                List<Object> found = foundArgs.get(expectedType);
                if (found == null) {
                    throw new IllegalStateException("Invalid arguments passed to CurryFunction, expected : " + expectedType);
                }
                finalArgs[i] = found.get(0);
                foundArgs.get(expectedType).remove(0);
                i++;
            }
            try {
                m.setAccessible(true);
                return (T) m.invoke(on, finalArgs);
            } catch (Exception ex) {
                throw new IllegalStateException("Error while running curryfied method", ex);
            }
        }

        @Override
        public CurryFunction<T> create(List<Object> args) {
            return new AutoCurryFunctionImpl<T>(m, on, args.toArray(new Object[args.size()]));
        }
    }

    public static <T, I> T target(I t, Class<T> contract) {
        if (!contract.isInterface()) {
            throw new IllegalStateException("Only works with interfaces");
        }
        return (T) Proxy.newProxyInstance(F.class.getClassLoader(),
                new Class[] {contract}, new CurryHandler(t, contract));
    }

    public static <T, R> CurryFunction<T> curry(T o) {
        if (CurryHandler.current.get() != null) {
            CurryHandler handler = CurryHandler.current.get();
            CurryHandler.current.remove();
            return new AutoCurryFunctionImpl<T>(handler.m, handler.o);
        } else {
            throw new IllegalStateException("You can't curry this object");
        }
    }

    private static class CurryHandler implements InvocationHandler {

        public static ThreadLocal<CurryHandler> current = new ThreadLocal<CurryHandler>();
        final Object o;
        final Class clazz;
        Method m;

        public CurryHandler(Object o, Class clazz) {
            this.o = o;
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            current.set(this);
            try {
                this.m = method;
                return null;
            } finally {
                //current.remove();
            }
        }
    }
}
