package cx.ath.mancel01.utils;

import java.util.Collections;
import java.util.Iterator;

/**
 * Utilities for everyday stuff.
 * 
 * Highly inspired by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public class F {

    final static None<Object> none = new None<Object>();
    
    public static interface Wrapper<T> extends Iterable<T> {
        
        boolean isDefined();

        boolean isEmpty();
        
        T get();

        Option<T> orElse(T value);
        
        T getOrElse(T value);
    }

    public static abstract class Option<T> implements Wrapper<T> {

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
                return (Option<A>) F.none;
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
            return new Either(Option.some(value), none);
        }

        public static <A, B> Either<A, B> right(B value) {
            return new Either(none, Option.some(value));
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
    
    public static interface MFunction<T, R> {
        
        Tuple<T, R> apply(Monad<T, ?> monad);
    }
    
    public static interface Monad<T, R> extends Wrapper<T> {
        
        Option<R> unit();
        
        Option<Object> error();
        
        void error(Object e);
                    
        <A> Monad<A, Object> pure(A a);
        
        <V> Monad<T, V> bind(MFunction<T, V> func);        
    }
    
    public static class Monadic<T, R> extends Option<T> implements Monad<T, R> {

        private T input;
        
        private Option<R> unit;
        
        private Option<Object> error;

        public Monadic(T input, Option<R> unit, Option<Object> error) {
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
        public <V> Monad<T, V> bind(MFunction<T, V> func) {
            try {
                Tuple<T, V> tuple = func.apply(this);
                T in = tuple._1;
                Option<V> o = Option.maybe(tuple._2);
                Option<Object> er = Option.none();
                if (error.isDefined()) {
                    er = Option.some(error.get());
                }
                return new Monadic<T, V>(in, o, er);
            } catch (Exception e) {
                Option<V> ret = Option.none();
                error = (Option) Option.some(e);
                return new Monadic<T, V>(input, ret, error);
            }
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
    }
}
