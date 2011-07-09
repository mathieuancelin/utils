package cx.ath.mancel01.utils;

import java.util.Collections;
import java.util.Iterator;

/**
 * Utilities for everyday stuff functionnal style.
 * 
 * Highly inspired by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public class F {
    
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

    final static None<Object> none = new None<Object>();

    public static abstract class Option<T> implements Iterable<T> {
        
        public abstract boolean isDefined();

        public abstract boolean isEmpty();
        
        public abstract T get();

        public abstract Option<T> orElse(T value);
        
        public abstract T getOrElse(T value);
        
        public abstract <R> Option<R> map(Function<T, R> function);
        
        public abstract Option<T> map(Action<T> function);
        
        public abstract <R> Option<R> map(CheckedFunction<T, R> function);
        
        public abstract Option<T> map(CheckedAction<T> function);

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

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Option<T> orElse(T value) {
            return Option.some(value);
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
        public T getOrElse(T value) {
            return this.value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Option<T> orElse(T value) {
            return this;
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
            return "Maybe ( " + input + " )";
        }

        @Override
        public boolean isEmpty() {
            return !isDefined();
        }

        @Override
        public Option<T> orElse(T value) {
            if (isDefined()) {
                return this;
            } else {
                return Option.some(value);
            }
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

        private Either(Option<A> left, Option<B> right) {
            this.left = left;
            this.right = right;
        }

        public static <A, B> Either<A, B> left(A value) {
            return new Either(Option.some(value), Option.none());
        }

        public static <A, B> Either<A, B> right(B value) {
            return new Either(Option.none(), Option.some(value));
        }

        public boolean isLeft() {
            return left.isDefined();
        }

        public boolean isRight() {
            return right.isDefined();
        }

        public Either<B, A> swap() {
            return new Either<B, A>(right,left);
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
    
    public static interface CF<C, R> {

        R _(C arg);
    }
    
    public static interface CCF<R> {
        
        R invoke();
    }
    
    public static <A, B, R> CF<A, CF<B, CCF<R>>> curry(final F2<A, B, R> function) {
        return new CF<A, CF<B, CCF<R>>>() {
            @Override
            public CF<B, CCF<R>> _(final A a) {
                return new CF<B, CCF<R>>() {
                    @Override
                    public CCF<R> _(final B b) {
                        return new CCF<R>() {
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
    
    public static <A, B, C, R> CF<A, CF<B, CF<C, CCF<R>>>> curry(final F3<A, B, C, R> function) {
        return new CF<A, CF<B, CF<C, CCF<R>>>>() {
            @Override
            public CF<B, CF<C, CCF<R>>> _(final A a) {
                return new CF<B, CF<C, CCF<R>>>() {
                    @Override
                    public CF<C, CCF<R>> _(final B b) {
                        return new CF<C, CCF<R>>() {
                            @Override
                            public CCF<R> _(final C c) {
                                return new CCF<R>() {
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
    
    public static <A, B, C, D, R> CF<A, CF<B, CF<C, CF<D, CCF<R>>>>> curry(final F4<A, B, C, D, R> function) {
        return new CF<A, CF<B, CF<C, CF<D, CCF<R>>>>>() {
            @Override
            public CF<B, CF<C, CF<D, CCF<R>>>> _(final A a) {
                return new CF<B, CF<C, CF<D, CCF<R>>>>() {
                    @Override
                    public CF<C, CF<D, CCF<R>>> _(final B b) {
                        return new CF<C, CF<D, CCF<R>>>() {
                            @Override
                            public CF<D, CCF<R>> _(final C c) {
                                return new CF<D, CCF<R>>() {
                                    @Override
                                    public CCF<R> _(final D d) {
                                        return new CCF<R>() {

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
    
    public static <A, B, C, D, E, R> CF<A, CF<B, CF<C, CF<D, CF<E, CCF<R>>>>>> curry(final F5<A, B, C, D, E, R> function) {
        return new CF<A, CF<B, CF<C, CF<D, CF<E, CCF<R>>>>>>() {
            @Override
            public CF<B, CF<C, CF<D, CF<E, CCF<R>>>>> _(final A a) {
                return new CF<B, CF<C, CF<D, CF<E, CCF<R>>>>>() {
                    @Override
                    public CF<C, CF<D, CF<E, CCF<R>>>> _(final B b) {
                        return new CF<C, CF<D, CF<E, CCF<R>>>>() {
                            @Override
                            public CF<D, CF<E, CCF<R>>> _(final C c) {
                                return new CF<D, CF<E, CCF<R>>>() {
                                    @Override
                                    public CF<E, CCF<R>> _(final D d) {
                                        return new CF<E, CCF<R>>() {
                                            @Override
                                            public CCF<R> _(final E e) {
                                                return new CCF<R>() {
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
    
    public static interface MRFunction<T, R> {
        
        Tuple<T, R> apply(Monad<T, ?> monad) throws Throwable;
    }
    
    public static interface MFunction<T, R> {
        
        R apply(Monad<T, ?> monad) throws Throwable;
    }
    
    public static interface Monad<T, R> {
        
        boolean isDefined();

        boolean isEmpty();
        
        T get();

        Option<T> orElse(T value);
        
        T getOrElse(T value);
        
        Option<R> unit();
        
        Option<Object> error();
        
        void error(Object e);
        
        void unit(R r);
                    
        <A> Monad<A, Object> pure(A a);
        
        <V> Monad<T, V> bind(MRFunction<T, V> func);
        
        <V> Monad<T, V> bind(MFunction<T, V> func);
       
        <V> Monad<T, V> bind(Function<T, V> func);
        
        <V> Monad<T, V> bind(Action<T> func);
        
        <V> Monad<T, V> bind(CheckedFunction<T, V> func);
        
        <V> Monad<T, V> bind(CheckedAction<T> func);
    }
    
    public static class Monadic<T, R> extends Option<T> implements Monad<T, R> {

        private T input;
        
        private Option<R> unit;
        
        private Option<Object> error;

        private Monadic(T input, Option<R> unit, Option<Object> error) {
            this.input = input;
            this.unit = unit;
            this.error = error;
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
        public Option<R> unit() {
            return unit;
        }

        @Override
        public String toString() {
            return "Monad ( " + input + " => " + unit + " : " + error + " )";
        }

        @Override
        public boolean isEmpty() {
            return !isDefined();
        }

        @Override
        public <A> Monad<A, Object> pure(A value) {
            return Monadic.monad(value);
        }

        @Override
        public <V> Monad<T, V> bind(MRFunction<T, V> func) {
            try {
                Tuple<T, V> tuple = func.apply(this);
                T in = tuple._1;
                Option<V> o = Option.maybe(tuple._2);
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(in, o, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
        }
        
        @Override
        public <V> Monad<T, V> bind(MFunction<T, V> func) {
            try {
                Option<V> o = Option.maybe(func.apply(this));
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(input, o, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
        }
        
        @Override
        public <V> Monad<T, V> bind(Function<T, V> func) {
            try {
                Option<V> out = Option.maybe(func.apply(get()));
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(this.input, out, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
        }
        
        @Override
        public <V> Monad<T, V> bind(Action<T> func) {
            try {
                func.apply(get());
                Option<V> out = Option.none();
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(this.input, out, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
        }
        
        @Override
        public <V> Monad<T, V> bind(final CheckedFunction<T, V> func) {
            try {
                Option<V> out = Option.maybe(func.apply(get()));
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(this.input, out, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
        }
        
        @Override
        public <V> Monad<T, V> bind(CheckedAction<T> func) {
            try {
                func.apply(get());
                Option<V> out = Option.none();
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(this.input, out, er);
            } catch (Throwable e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
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
        public Option<T> orElse(T value) {
            if (isDefined()) {
                return this;
            } else {
                return Option.some(value);
            }
        }
                
        public static <T> Monad<T, Object> monad(T value) {
            return new Monadic<T, Object>(value, Option.none(), Option.none());
        }

        @Override
        public Option<Object> error() {
            return error;
        }

        @Override
        public void error(Object e) {
            error = Option.some(e);
        }

        @Override
        public void unit(R r) {
            unit = Option.maybe(r);
        }
    }
}
