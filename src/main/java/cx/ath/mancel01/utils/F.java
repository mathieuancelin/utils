package cx.ath.mancel01.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utilities for everyday stuff.
 * 
 * Highly inspired by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public class F {

    final static None<Object> none = new None<Object>();

    public static abstract class Option<T> implements Iterable<T> {

        public abstract boolean isDefined();

        public abstract T get();

        public abstract T getOrElse(T value);

        public static <T> None<T> none() {
            return (None<T>) (Object) none;
        }

        public static <T> Some<T> some(T value) {
            return new Some<T>(value);
        }

        public static <T> Maybe<T> maybe(T value) {
            return new Maybe<T>(value);
        }
    }

    public static class None<T> extends Option<T> {

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public T get() {
            throw new IllegalStateException("No value");
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "None";
        }

        @Override
        public T getOrElse(T value) {
            return value;
        }
    }

    public static class Some<T> extends Option<T> {

        final T value;

        public Some(T value) {
            if (value == null) {
                throw new IllegalStateException("Null value");
            }
            this.value = value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }

        @Override
        public T getOrElse(T value) {
            return this.value;
        }
    }

    public static class Any<T> extends Some<Object> {

        public Any(T value) {
            super(value);
        }

        public Class<?> type() {
            return value.getClass();
        }

        public boolean isTyped(Class<?> type) {
            return type.isAssignableFrom(type());
        }

        public <A> Option<A> typed(Class<A> type) {
            if (isTyped(type)) {
                return F.some(type.cast(value));
            } else {
                return (Option<A>) F.none;
            }
        }
    }

    public static class Either<A, B> {

        final public Option<A> _1;
        final public Option<B> _2;

        private Either(Option<A> _1, Option<B> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static <A, B> Either<A, B> _1(A value) {
            return new Either(some(value), none);
        }

        public static <A, B> Either<A, B> _2(B value) {
            return new Either(none, some(value));
        }

        @Override
        public String toString() {
            return "E2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class Tuple<A, B> {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public String toString() {
            return "T2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static class Maybe<T> extends Option<T> {

        private final T input;

        public Maybe(T input) {
            this.input = input;
        }

        @Override
        public boolean isDefined() {
            return !(input == null);
        }

        @Override
        public T get() {
            return input;
        }

        @Override
        public T getOrElse(T value) {
            if (input == null) {
                return value;
            } else {
                return input;
            }
        }

        @Override
        public Iterator<T> iterator() {
            if (input == null) {
                return Collections.<T>emptyList().iterator();
            } else {
                return Collections.singletonList(input).iterator();
            }
        }

        @Override
        public String toString() {
            return "Maybe(" + input + ")";
        }
    }

    public static interface CurryFunction<T> {

        T apply();

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
        public T apply() {
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
    
    public static <T, R> CurryFunction<R> curry(T o, Class<R> expect) {
        if (CurryHandler.current.get() != null) {
            CurryHandler handler = CurryHandler.current.get();
            CurryHandler.current.remove();
            return new AutoCurryFunctionImpl<R>(handler.m, handler.o);
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

    public static <A, B> Tuple<A, B> tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static <A> Some<A> some(A a) {
        return new Some(a);
    }

    public static <A> Maybe<A> maybe(A a) {
        return new Maybe(a);
    }
}
