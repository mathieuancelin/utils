/*
 *  Copyright 2011-2012 Mathieu ANCELIN
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

import java.lang.reflect.Constructor;

public class L {

    public static <T> Act<T> newSimpleClosure(Class<?> closure) {
        try {
            Act action = new Act();
            action.applyingClass(closure);
            return (Act<T>) (Object) action;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, R> Func<T, R> newClosure(Class<?> closure) {
        try {
            Func function = new Func();
            function.applyingClass(closure);
            return (Func<T, R>) (Object) function;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static abstract class Invocable<T> {

        static ThreadLocal inHolder = new ThreadLocal();

        private Class<?> applyingClass;

        private Constructor[] constructors;

        public T in() {
            return (T) inHolder.get();
        }

        void applyingClass(Class<?> applyingClass) {
            this.applyingClass = applyingClass;
            this.constructors = applyingClass.getDeclaredConstructors();
        }

        Constructor[] constructors() {
            return constructors;
        }

        Class<?> applyingClass() {
            return applyingClass;
        }
    }

    public static class Act<T> extends Invocable<T> implements F.Action<T> {

        public final void apply() {
            apply(null);
        }

        public final void apply(T in) {
            inHolder.set(in);
            if (constructors() != null) {
                Constructor c = constructors()[0];
                if (c != null) {
                    try {
                        Object[] args = new Object[1];
                        args[0] = null;
                        if (!c.isAccessible()) {
                            c.setAccessible(true);
                        }
                        c.newInstance(args);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            inHolder.remove();
        }
    }

    public static class Func<T, R> extends Invocable<T> implements F.Function<T, R> {

        protected R out;

        public final R apply() {
            return apply(null);
        }

        public final R apply(T in) {
            inHolder.set(in);
            if (constructors() != null) {
                Constructor c = constructors()[0];
                if (c != null) {
                    try {
                        Object[] args = new Object[1];
                        args[0] = null;
                        if (!c.isAccessible()) {
                            c.setAccessible(true);
                        }
                        Object instance = c.newInstance(args);
                        if (Func.class.isAssignableFrom(instance.getClass())) {
                            return (R) ((Func) instance).out;
                        } else {
                            return null;
                        }
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
            inHolder.remove();
            return null;
        }
    }
}
