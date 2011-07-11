/*
 *  Copyright 2011 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.F10;
import cx.ath.mancel01.utils.F.F2;
import cx.ath.mancel01.utils.F.F3;
import cx.ath.mancel01.utils.F.F4;
import cx.ath.mancel01.utils.F.F5;
import cx.ath.mancel01.utils.F.F6;
import cx.ath.mancel01.utils.F.F7;
import cx.ath.mancel01.utils.F.F8;
import cx.ath.mancel01.utils.F.F9;
import cx.ath.mancel01.utils.F.Option;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Attempt to support some kind of curryfication for Java.
 * Very useful for partial method application.
 *
 * @author Mathieu ANCELIN
 */
public final class Y {

    private Y() {}


    public static interface P<C, R> {

        R _(C arg);
    }

    public static interface Inv<R> {

        R invoke();
    }

    public static <A, B, R> P<A, P<B, Inv<R>>> curry(final F2<A, B, R> function) {
        return new P<A, P<B, Inv<R>>>() {
            @Override
            public P<B, Inv<R>> _(final A a) {
                return new P<B, Inv<R>>() {
                    @Override
                    public Inv<R> _(final B b) {
                        return new Inv<R>() {
                            @Override
                            public R invoke() {
                                return function.apply(a, b);
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, R> P<A, P<B, P<C, Inv<R>>>> curry(final F3<A, B, C, R> function) {
        return new P<A, P<B, P<C, Inv<R>>>>() {
            @Override
            public P<B, P<C, Inv<R>>> _(final A a) {
                return new P<B, P<C, Inv<R>>>() {
                    @Override
                    public P<C, Inv<R>> _(final B b) {
                        return new P<C, Inv<R>>() {
                            @Override
                            public Inv<R> _(final C c) {
                                return new Inv<R>() {
                                    @Override
                                    public R invoke() {
                                        return function.apply(a, b, c);
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, R> P<A, P<B, P<C, P<D, Inv<R>>>>> curry(final F4<A, B, C, D, R> function) {
        return new P<A, P<B, P<C, P<D, Inv<R>>>>>() {
            @Override
            public P<B, P<C, P<D, Inv<R>>>> _(final A a) {
                return new P<B, P<C, P<D, Inv<R>>>>() {
                    @Override
                    public P<C, P<D, Inv<R>>> _(final B b) {
                        return new P<C, P<D, Inv<R>>>() {
                            @Override
                            public P<D, Inv<R>> _(final C c) {
                                return new P<D, Inv<R>>() {
                                    @Override
                                    public Inv<R> _(final D d) {
                                        return new Inv<R>() {

                                            @Override
                                            public R invoke() {
                                                return function.apply(a, b, c, d);
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, R> P<A, P<B, P<C, P<D, P<E, Inv<R>>>>>> curry(final F5<A, B, C, D, E, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, Inv<R>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, Inv<R>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, Inv<R>>>>>() {
                    @Override
                    public P<C, P<D, P<E, Inv<R>>>> _(final B b) {
                        return new P<C, P<D, P<E, Inv<R>>>>() {
                            @Override
                            public P<D, P<E, Inv<R>>> _(final C c) {
                                return new P<D, P<E, Inv<R>>>() {
                                    @Override
                                    public P<E, Inv<R>> _(final D d) {
                                        return new P<E, Inv<R>>() {
                                            @Override
                                            public Inv<R> _(final E e) {
                                                return new Inv<R>() {
                                                    @Override
                                                    public R invoke() {
                                                        return function.apply(a, b, c, d, e);
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, F, R>
        P<A, P<B, P<C, P<D, P<E, P<F, Inv<R>>>>>>>
            curry(final F6<A, B, C, D, E, F, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, P<F, Inv<R>>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, P<F, Inv<R>>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, P<F, Inv<R>>>>>>() {
                    @Override
                    public P<C, P<D, P<E, P<F, Inv<R>>>>> _(final B b) {
                        return new P<C, P<D, P<E, P<F, Inv<R>>>>>() {
                            @Override
                            public P<D, P<E, P<F, Inv<R>>>> _(final C c) {
                                return new P<D, P<E, P<F, Inv<R>>>>() {
                                    @Override
                                    public P<E, P<F, Inv<R>>> _(final D d) {
                                        return new P<E, P<F, Inv<R>>>() {
                                            @Override
                                            public P<F, Inv<R>> _(final E e) {
                                                return new P<F, Inv<R>>() {
                                                    @Override
                                                    public Inv<R> _(final F f) {
                                                        return new Inv<R>() {
                                                            @Override
                                                            public R invoke() {
                                                                return function.apply(a, b, c, d, e, f);
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, F, G, R>
        P<A, P<B, P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>>>>
            curry(final F7<A, B, C, D, E, F, G, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>>>() {
                    @Override
                    public P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>> _(final B b) {
                        return new P<C, P<D, P<E, P<F, P<G, Inv<R>>>>>>() {
                            @Override
                            public P<D, P<E, P<F, P<G, Inv<R>>>>> _(final C c) {
                                return new P<D, P<E, P<F, P<G, Inv<R>>>>>() {
                                    @Override
                                    public P<E, P<F, P<G, Inv<R>>>> _(final D d) {
                                        return new P<E, P<F, P<G, Inv<R>>>>() {
                                            @Override
                                            public P<F, P<G, Inv<R>>> _(final E e) {
                                                return new P<F, P<G, Inv<R>>>() {
                                                    @Override
                                                    public P<G, Inv<R>> _(final F f) {
                                                        return new P<G, Inv<R>>() {
                                                            @Override
                                                            public Inv<R> _(final G g) {
                                                                return new Inv<R>() {
                                                                    @Override
                                                                    public R invoke() {
                                                                        return function.apply(a, b, c, d, e, f, g);
                                                                    }
                                                                };
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, F, G, H, R>
        P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>>>>
            curry(final F8<A, B, C, D, E, F, G, H, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>>>() {
                    @Override
                    public P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>> _(final B b) {
                        return new P<C, P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>>() {
                            @Override
                            public P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>> _(final C c) {
                                return new P<D, P<E, P<F, P<G, P<H, Inv<R>>>>>>() {
                                    @Override
                                    public P<E, P<F, P<G, P<H, Inv<R>>>>> _(final D d) {
                                        return new P<E, P<F, P<G, P<H, Inv<R>>>>>() {
                                            @Override
                                            public P<F, P<G, P<H, Inv<R>>>> _(final E e) {
                                                return new P<F, P<G, P<H, Inv<R>>>>() {
                                                    @Override
                                                    public P<G, P<H, Inv<R>>> _(final F f) {
                                                        return new P<G, P<H, Inv<R>>>() {
                                                            @Override
                                                            public P<H, Inv<R>> _(final G g) {
                                                                return new P<H, Inv<R>>() {
                                                                    @Override
                                                                    public Inv<R> _(final H h) {
                                                                        return new Inv<R>() {
                                                                            @Override
                                                                            public R invoke() {
                                                                                return function.apply(a, b, c, d, e, f, g, h);
                                                                            }
                                                                        };
                                                                    }
                                                                };
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, F, G, H, I, R>
        P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>>>>
            curry(final F9<A, B, C, D, E, F, G, H, I, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>>>() {
                    @Override
                    public P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>> _(final B b) {
                        return new P<C, P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>>() {
                            @Override
                            public P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>> _(final C c) {
                                return new P<D, P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>>() {
                                    @Override
                                    public P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>> _(final D d) {
                                        return new P<E, P<F, P<G, P<H, P<I, Inv<R>>>>>>() {
                                            @Override
                                            public P<F, P<G, P<H, P<I, Inv<R>>>>> _(final E e) {
                                                return new P<F, P<G, P<H, P<I, Inv<R>>>>>() {
                                                    @Override
                                                    public P<G, P<H, P<I, Inv<R>>>> _(final F f) {
                                                        return new P<G, P<H, P<I, Inv<R>>>>() {
                                                            @Override
                                                            public P<H, P<I, Inv<R>>> _(final G g) {
                                                                return new P<H, P<I, Inv<R>>>() {
                                                                    @Override
                                                                    public P<I, Inv<R>> _(final H h) {
                                                                        return new P<I, Inv<R>>() {
                                                                            @Override
                                                                            public Inv<R> _(final I i) {
                                                                                return new Inv<R>() {
                                                                                    @Override
                                                                                    public R invoke() {
                                                                                        return function.apply(a, b, c, d, e, f, g, h, i);
                                                                                    }
                                                                                };
                                                                            }
                                                                        };
                                                                    }
                                                                };
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <A, B, C, D, E, F, G, H, I, J, R>
        P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>>>>
            curry(final F10<A, B, C, D, E, F, G, H, I, J, R> function) {
        return new P<A, P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>>>>() {
            @Override
            public P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>>> _(final A a) {
                return new P<B, P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>>>() {
                    @Override
                    public P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>> _(final B b) {
                        return new P<C, P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>>() {
                            @Override
                            public P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>> _(final C c) {
                                return new P<D, P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>>() {
                                    @Override
                                    public P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>> _(final D d) {
                                        return new P<E, P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>>() {
                                            @Override
                                            public P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>> _(final E e) {
                                                return new P<F, P<G, P<H, P<I, P<J, Inv<R>>>>>>() {
                                                    @Override
                                                    public P<G, P<H, P<I, P<J, Inv<R>>>>> _(final F f) {
                                                        return new P<G, P<H, P<I, P<J, Inv<R>>>>>() {
                                                            @Override
                                                            public P<H, P<I, P<J, Inv<R>>>> _(final G g ) {
                                                                return new P<H, P<I, P<J, Inv<R>>>>() {
                                                                    @Override
                                                                    public P<I, P<J, Inv<R>>> _(final H h) {
                                                                        return new P<I, P<J, Inv<R>>>() {
                                                                            @Override
                                                                            public P<J, Inv<R>> _(final I i) {
                                                                                return new P<J, Inv<R>>() {
                                                                                    @Override
                                                                                    public Inv<R> _(final J j) {
                                                                                        return new Inv<R>() {
                                                                                            @Override
                                                                                            public R invoke() {
                                                                                                return function.apply(a, b, c, d, e, f, g, h, i, j);
                                                                                            }
                                                                                        };
                                                                                    }
                                                                                };
                                                                            }
                                                                        };
                                                                    }
                                                                };
                                                            }
                                                        };
                                                    }
                                                };
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    public static <T, R> CurryFunction<T> curryM(T o) {
        if (CurryHandler.current.get() != null) {
            CurryHandler handler = CurryHandler.current.get();
            CurryHandler.current.remove();
            return new AutoCurryFunctionImpl<T>(handler.m, handler.o);
        } else {
            throw new IllegalStateException("You can't curry this object");
        }
    }

    public static <T> CurryFunction<T> curryMethod(Option<CurryMethod<T>> m, Object... with) {
        if (m.isDefined()) {
            return new AutoCurryFunctionImpl<T>(m.get().m, m.get().on, with);
        } else {
            throw new IllegalStateException("Method unavailable.");
        }
    }

    /**
     * Creates a mock usable with method curry(T o) to target a specific
     * method to curry with :
     * <code>
     *      Person p = target(new Person());
     *      curry(p.fill(n, n, n, n))._("John")._("Doe")._("Boston")._("USA").get();
     * </code>
     * 
     * @param <T>
     * @param <I>
     * @param t
     * @param contracts
     * @return
     */
    public static <T, I> T target(I t, Class<T>... contracts) {
        if (contracts == null || contracts.length == 0) {
            try {
                Class<?> clazz = getProxyClass(t.getClass());
                if (clazz == null) {
                    throw new IllegalStateException("Error while creating proxy for " + t);
                }
                Object o = clazz.newInstance();
                InvocationHandler h = new CurryHandler(t, new Class[] {t.getClass()});
                ((Y.CustomProxy) o).setFrom(t.getClass());
                ((Y.CustomProxy) o).setHandler(h);
                return (T) o;
            } catch (Exception ex) {
                throw new IllegalStateException("Error while creating proxy for " + t, ex);
            }
        }
        return (T) Proxy.newProxyInstance(F.class.getClassLoader(),
                contracts, new CurryHandler(t, contracts));
    }

    public static <T> Option<CurryMethod<T>> method(Object on, Class<T> ret, String methodName, Class<?>... args) {
        Method m = null;
        try {
            m = on.getClass().getDeclaredMethod(methodName, args);
            return Option.some(new CurryMethod<T>(m, on, ret, args));
        } catch (NoSuchMethodException ex) {
            return Option.none();
        }
    }

    public static class CurryMethod<T> {
        final Method m;
        final Object on;
        final Class<T> ret;
        final Object[] args;

        public CurryMethod(Method m, Object on, Class<T> ret, Object[] args) {
            this.m = m;
            this.on = on;
            this.ret = ret;
            this.args = args;
        }
    }

    public static interface CurryFunction<T> {

        T get();

        <P> CurryFunction<T> _(P arg);
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

    private static class CurryHandler implements InvocationHandler {

        public static ThreadLocal<CurryHandler> current = new ThreadLocal<CurryHandler>();
        final Object o;
        final Class<?>[] classes;
        Method m;

        public CurryHandler(Object o, Class<?>[] classes) {
            this.o = o;
            this.classes = classes;
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

    public static class Null {

        public static <T> T type(Class<T> type) {
            return null;
        }
    }

    /***************************************************************************
     *
     * WARNING : Highly dirty and experimental stuff, use with caution.
     *
     **************************************************************************/

    public static interface CustomProxy {
        
        InvocationHandler getHandler();

        void setFrom(Class<?> from);

        void setHandler(InvocationHandler handler);
    }

    static <T> Class<?> getProxyClass(Class<T> from) {
        String className = "Proxied_YClass_" + (managedClasses.size() + 1) + "_" + from.getSimpleName();
        String packageName = from.getPackage().getName();
        if (managedClasses.containsKey(packageName + "." + className)) {
            return managedClasses.get(packageName + "." + className);
        }
        if (from.getModifiers() == Modifier.FINAL) {
            throw new IllegalStateException("Can't proxy final class.");
        }
        
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("package ");
        codeBuilder.append(packageName);
        codeBuilder.append("; \n\n");

        codeBuilder.append("import cx.ath.mancel01.utils.Y.CustomProxy;\n");
        codeBuilder.append("import java.lang.reflect.InvocationHandler;\n");
        codeBuilder.append("import java.lang.reflect.Method;\n\n");

        codeBuilder.append("public class ")
                .append(className)
                .append(" extends ")
                .append(from.getName())
                .append(" implements CustomProxy {\n\n");

        codeBuilder.append("private InvocationHandler handler;\nprivate Class<?> from;\n\n");
        codeBuilder.append("@Override\n");
        codeBuilder.append("public InvocationHandler getHandler() {\n");
        codeBuilder.append("return handler;\n");
        codeBuilder.append("}\n");
        codeBuilder.append("@Override\n");
        codeBuilder.append("public void setHandler(InvocationHandler handler) {\n");
        codeBuilder.append("this.handler = handler;\n");
        codeBuilder.append("}\n");
        codeBuilder.append("@Override\n");
        codeBuilder.append("public void setFrom(Class<?> from) {\n");
        codeBuilder.append("this.from = from;\n");
        codeBuilder.append("}\n");
        codeBuilder.append("private Object call(String name, Class<?>[] parametersType, Object... args) {\n");
        codeBuilder.append("try {\n");
        codeBuilder.append("Method m = from.getDeclaredMethod(name, parametersType);\n");
        codeBuilder.append("return handler.invoke(this, m, args);\n");
        codeBuilder.append("} catch (Throwable ex) {\n");
        codeBuilder.append("throw new RuntimeException(ex);\n");
        codeBuilder.append("}\n}\n");

        for (Method m : from.getMethods()) {
            if (!Modifier.isFinal(m.getModifiers())) {
                int i = 0;
                StringBuilder methodBuilder = new StringBuilder();
                StringBuilder typesBuilder = new StringBuilder();
                StringBuilder argsBuilder = new StringBuilder();

                for (Class<?> type : m.getParameterTypes()) {
                    if (i > 0) {
                        methodBuilder.append(", ");
                        typesBuilder.append(", ");
                        argsBuilder.append(", ");
                    }
                    methodBuilder.append(type.getName()).append(" param").append(i);
                    if (type.isPrimitive()) {
                        typesBuilder.append(getType(type.getName()).getName()).append(".class");
                    } else {
                        typesBuilder.append(type.getName()).append(".class");
                    }
                    argsBuilder.append("param").append(i);
                    i++;
                }
                codeBuilder.append("public ");
                codeBuilder.append(m.getReturnType().getName());
                codeBuilder.append(" ").append(m.getName()).append("(")
                        .append(methodBuilder.toString()).append(") {\n");
                if (!m.getReturnType().getName().equals("void")) {
                    if (m.getReturnType().isPrimitive()) {
                        codeBuilder.append("return ").append(
                                getType(m.getReturnType().getName()).getName())
                                .append(".class.cast").append("(");
                    } else {
                        codeBuilder.append("return ").append(
                                m.getReturnType().getName())
                                .append(".class.cast").append("(");
                    }
                }
                codeBuilder.append(" call(\"").append(m.getName())
                        .append("\", new Class<?>[] {")
                        .append(typesBuilder.toString()).append("}");
                if (i > 0)
                    codeBuilder.append(", ");
                codeBuilder.append(argsBuilder.toString()).append(")");
                if (!m.getReturnType().equals(Void.TYPE)) {
                    codeBuilder.append(")");
                }
                codeBuilder.append(";\n");
                codeBuilder.append("}\n\n");
            }
        }
        codeBuilder.append("}\n");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector =
                new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnosticsCollector, null, null);
        JavaFileObject javaObjectFromString;
        try {
            javaObjectFromString = new JavaObjectFromString(className + ".java", codeBuilder.toString());
            Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(javaObjectFromString);
            CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, null, null, fileObjects);
            Boolean result = task.call();
            List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
            if (!result) {
                System.out.println(codeBuilder.toString());
           
                for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
                    System.out.println(d);
                }
            } else {
                managedClasses.put(packageName + "." + className, Y.class);
                return loader.loadClass(packageName + "." + className);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static Class<?> getType(String primitive) {
        if (primitive.equals("byte")) {
            return Byte.class;
        } else if (primitive.equals("short")) {
            return Short.class;
        } else if (primitive.equals("int")) {
            return Integer.class;
        } else if (primitive.equals("long")) {
            return Long.class;
        } else if (primitive.equals("float")) {
            return Float.class;
        } else if (primitive.equals("double")) {
            return Double.class;
        } else if (primitive.equals("char")) {
            return Character.class;
        } else if (primitive.equals("boolean")) {
            return Boolean.class;
        } else {
            return Void.class;
        }
    }

    private static final Map<String, Class<?>> managedClasses = new HashMap<String, Class<?>>();
    private static final ClassLoader loader = new CustomClassLoader(Y.class.getClassLoader());

    private static class CustomClassLoader extends ClassLoader {

        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (managedClasses.containsKey(name) && managedClasses.get(name).equals(Y.class)) {
                byte[] b = getClassDefinition(new File(name.substring(name.lastIndexOf(".") + 1) + ".class"));
                new File(name.substring(name.lastIndexOf(".") + 1) + ".class").delete();
                return defineClass(name, b, 0, b.length);
            } else if (managedClasses.containsKey(name) && !managedClasses.get(name).equals(Y.class)) {
                return managedClasses.get(name);
            } else {
                return super.loadClass(name);
            }
        }

        public static byte[] getClassDefinition(File file) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            if (is == null) {
                return null;
            }
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = is.read(buffer, 0, buffer.length)) > 0) {
                    os.write(buffer, 0, count);
                }
                return os.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class JavaObjectFromString extends SimpleJavaFileObject {

        private String contents = null;

        public JavaObjectFromString(String className, String contents) throws Exception {
            super(new URI(className), Kind.SOURCE);
            this.contents = contents;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }
    }
}
