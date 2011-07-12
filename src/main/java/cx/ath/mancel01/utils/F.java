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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;

/**
 * Utilities for everyday stuff functional style.
 * 
 * Highly inspired by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public final class F {

    final static None<Object> none = new None<Object>();

    public static final Void VOID = null;

    private F() {}

    public static interface Callable<T> {

        T apply();
    }

    public static interface Action<T> {

        void apply(T t);
    }
    
    public static interface CheckedAction<T> {

        void apply(T t) throws Throwable;
    }

    public static interface Function<T, R> {

        R apply(T t);
    }
        
    public static interface CheckedFunction<T, R> {

        R apply(T t) throws Throwable;
    }

    public static interface Monad<T> {

        <R> Option<R> map(Function<T, R> function);

        Option<T> map(Action<T> function);

        <R> Option<R> map(CheckedFunction<T, R> function);

        Option<T> map(CheckedAction<T> function);

        Option<T> bind(Action<Option<T>> action);

        Option<T> bind(CheckedAction<Option<T>> action);
    }

    public static abstract class Option<T> implements Iterable<T>, Monad<T> {
        
        public abstract boolean isDefined();

        public abstract boolean isEmpty();
        
        public abstract T get();

        public Option<T> orElse(T value) {
            return isEmpty() ? Option.maybe(value) : this;
        }
        
        public T getOrElse(T value) {
            return isEmpty() ? value : get();
        }

        public T getOrElse(Function<Void, T> function) {
            return isEmpty() ? function.apply(VOID) : get();
        }

        public T getOrElse(Callable<T> function) {
            return isEmpty() ? function.apply() : get();
        }

        public T getOrNull() {
            return isEmpty() ? null : get();
        }

        public <X> Either<X, T> toRight(X left) {
             return new Either<X, T>(Option.maybe(left), this);
        }

        public <X> Either<T, X> toLeft(X right) {
             return new Either<T, X>(this, Option.maybe(right));
        }

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
        public boolean isEmpty() {
            return true;
        }

        @Override
        public <R> Option<R> map(Function<T, R> function) {
            return Option.none();
        }

        @Override
        public Option<T> map(Action<T> function) {
            return Option.none();
        }
        
        @Override
        public <R> Option<R> map(CheckedFunction<T, R> function) {
            return Option.none();
        }

        @Override
        public Option<T> map(CheckedAction<T> function) {
            return Option.none();
        }

        @Override
        public Option<T> bind(Action<Option<T>> action) {
            return Option.none();
        }

        @Override
        public Option<T> bind(CheckedAction<Option<T>> action) {
            return Option.none();
        }
    }

    public static class Some<T> extends Option<T> {

        final T value;

        public Some(T value) {
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
            return "Some ( " + value + " )";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public <R> Option<R> map(Function<T, R> function) {
            try {
                return Option.maybe(function.apply(get()));
            } catch (Throwable t) {
                return Option.none();
            }
        }

        @Override
        public Option<T> map(Action<T> function) {
            try {
                function.apply(get());
                return Option.maybe(get());
            } catch (Throwable t) {
                return Option.none();
            }
        }
        
        @Override
        public <R> Option<R> map(CheckedFunction<T, R> function) {
            try {
                return Option.maybe(function.apply(get()));
            } catch (Throwable t) {
                return Option.none();
            }
        }

        @Override
        public Option<T> map(CheckedAction<T> function) {
            try {
                function.apply(get());
                return Option.maybe(get());
            } catch (Throwable t) {
                return Option.none();
            }
        }

        @Override
        public Option<T> bind(Action<Option<T>> action) {
            try {
                action.apply(this);
            } catch (Throwable t) {
            }
            return this;
        }

        @Override
        public Option<T> bind(CheckedAction<Option<T>> action) {
            try {
                action.apply(this);
            } catch (Throwable t) {
            }
            return this;
        }
    }
    
    /**
     * A not so good version of some. Mostly used to wrap
     * return of library methods.
     *
     * @param <T>
     */
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
        public Iterator<T> iterator() {
            if (input == null) {
                return Collections.<T>emptyList().iterator();
            } else {
                return Collections.singletonList(input).iterator();
            }
        }

        @Override
        public String toString() {
            return "Maybe ( " + input + " )";
        }

        @Override
        public boolean isEmpty() {
            return !isDefined();
        }
 
        @Override
        public <R> Option<R> map(Function<T, R> function) {
            if (isDefined()) {
                try {
                    return Option.maybe(function.apply(get()));
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }

        @Override
        public Option<T> map(Action<T> function) {
            if (isDefined()) {
                try {
                    function.apply(get());
                    return Option.maybe(get());
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }
        
        @Override
        public <R> Option<R> map(CheckedFunction<T, R> function) {
            if (isDefined()) {
                try {
                    return Option.maybe(function.apply(get()));
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }

        @Override
        public Option<T> map(CheckedAction<T> function) {
            if (isDefined()) {
                try {
                    function.apply(get());
                    return Option.maybe(get());
                } catch (Throwable t) {
                    return Option.none();
                }
            }
            return Option.none();
        }

        @Override
        public Option<T> bind(Action<Option<T>> action) {
            if (isDefined()) {
                try {
                    action.apply(this);
                    return this;
                } catch (Throwable t) {
                    return this;
                }
            }
            return Option.none();
        }

        @Override
        public Option<T> bind(CheckedAction<Option<T>> action) {
           if (isDefined()) {
                try {
                    action.apply(this);
                    return this;
                } catch (Throwable t) {
                    return this;
                }
            }
            return Option.none();
        }
    }

    public static class Any<T> extends Some<T> {

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
                return Option.some(type.cast(value));
            } else {
                return Option.none();
            }
        }
    }

    public static class Either<A, B> {

        final public Option<A> left;
        final public Option<B> right;

        private Either(A left, B right) {
            this.left = Option.maybe(left);
            this.right = Option.maybe(right);
        }

        private Either(Option<A> left, Option<B> right) {
            this.left = left;
            this.right = right;
        }

        public static <A, B> Either<A, B> left(A value) {
            return new Either<A, B>(Option.maybe(value), (Option<B>) Option.none());
        }

        public static <A, B> Either<A, B> right(B value) {
            return new Either<A, B>((Option<A>) Option.none(), Option.maybe(value));
        }

        public <X> Option<X> fold(Function<A, X> fa, Function<B,X> fb) {
            if (isLeft()) {
                return Option.maybe(fa.apply(left.get()));
            } else if (isRight()) {
                return Option.maybe(fb.apply(right.get()));
            } else {
                return (Option<X>) Option.none();
            }
        }

        public boolean isLeft() {
            return left.isDefined();
        }

        public boolean isRight() {
            return right.isDefined();
        }

        public Either<B, A> swap() {
            return new Either<B, A>(right, left);
        }

        @Override
        public String toString() {
            return "Either ( left: " + left + ", right: " + right + " )";
        }
    }

    public static class Tuple<A, B> {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public Tuple<B, A> swap() {
            return new Tuple<B, A>(_2, _1);
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + " )";
        }
    }

    public static interface F2<A, B, R> {

        R apply(A a, B b);
    }

    public static interface F3<A, B, C, R> {

        R apply(A a, B b, C c);
    }

    public static interface F4<A, B, C, D, R> {

        R apply(A a, B b, C c, D d);
    }

    public static interface F5<A, B, C, D, E, R> {

        R apply(A a, B b, C c, D d, E e);
    }

    public static interface F6<A, B, C, D, E, F, R> {

        R apply(A a, B b, C c, D d, E e, F f);
    }

    public static interface F7<A, B, C, D, E, F, G, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g);
    }

    public static interface F8<A, B, C, D, E, F, G, H, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    public static interface F9<A, B, C, D, E, F, G, H, I, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    public static interface F10<A, B, C, D, E, F, G, H, I, J, R> {

        R apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    public static class Tuple3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public Tuple3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3 + " )";
        }
    }

    public static class Tuple4<A, B, C, D> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;

        public Tuple4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: "
                + _3 + ", _4: " + _4 + " )";
        }
    }

    public static class Tuple5<A, B, C, D, E> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;

        public Tuple5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + " )";
        }
    }

    public static class Tuple6<A, B, C, D, E, F> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;

        public Tuple6(A _1, B _2, C _3, D _4, E _5, F _6) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6 + " )";
        }
    }

    public static class Tuple7<A, B, C, D, E, F, G> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;

        public Tuple7(A _1, B _2, C _3, D _4, E _5, F _6, G _7) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6 + " )";
        }
    }

    public static class Tuple8<A, B, C, D, E, F, G, H> {
        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;

        public Tuple8(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8
                + " )";
        }
    }

    public static class Tuple9<A, B, C, D, E, F, G, H, I> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;
        final public I _9;

        public Tuple9(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8, I _9) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8 + ", _9: " + _9
                + " )";
        }
    }

    public static class Tuple10<A, B, C, D, E, F, G, H, I, J> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;
        final public F _6;
        final public G _7;
        final public H _8;
        final public I _9;
        final public J _10;

        public Tuple10(A _1, B _2, C _3, D _4, E _5, F _6, G _7, H _8, I _9, J _10) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
            this._6 = _6;
            this._7 = _7;
            this._8 = _8;
            this._9 = _9;
            this._10 = _10;
        }

        @Override
        public String toString() {
            return "Tuple ( _1: " + _1 + ", _2: " + _2 + ", _3: " + _3
                + ", _4: " + _4 + ", _5: " + _5 + ", _6: " + _6
                + ", _7: " + _7 + ", _8: " + _8 + ", _9: " + _9
                + ", _10: " + _10 + " )";
        }
    }

    public static  class UncheckedAction<T> implements Action<T> {

        private final CheckedAction<T> action;

        public UncheckedAction(CheckedAction<T> action) {
            this.action = action;
        }

        @Override
        public void apply(T param) {
            try {
                action.apply(param);
            } catch (Throwable t) {
                throw new ExceptionWrapper(t);
            }
        }
    }

    public static class UncheckedFunction<T, R> implements Function<T, R> {

        private final CheckedFunction<T, R> function;

        public UncheckedFunction(CheckedFunction<T, R> function) {
            this.function = function;
        }

        @Override
        public R apply(T param) {
            try {
                return function.apply(param);
            } catch (Throwable ex) {
                throw new ExceptionWrapper(ex);
            }
        }
    }

    public static class ExceptionWrapper extends RuntimeException {

        private final Throwable t;

        public ExceptionWrapper(Throwable t) {
            this.t = t;
        }

        @Override
        public String getMessage() {
            return t.getMessage();
        }

        @Override
        public String getLocalizedMessage() {
            return t.getLocalizedMessage();
        }

        @Override
        public Throwable getCause() {
            return t.getCause();
        }

        @Override
        public synchronized Throwable initCause(Throwable throwable) {
            return t.initCause(throwable);
        }

        @Override
        public String toString() {
            return t.toString();
        }

        @Override
        public void printStackTrace() {
            t.printStackTrace();
        }

        @Override
        public void printStackTrace(PrintStream printStream) {
            t.printStackTrace(printStream);
        }

        @Override
        public void printStackTrace(PrintWriter printWriter) {
            t.printStackTrace(printWriter);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return t.fillInStackTrace();
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return t.getStackTrace();
        }

        @Override
        public void setStackTrace(StackTraceElement[] stackTraceElements) {
            t.setStackTrace(stackTraceElements);
        }
    }
}
