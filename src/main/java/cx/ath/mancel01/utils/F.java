package cx.ath.mancel01.utils;

import java.util.Collections;
import java.util.Iterator;

/**
 * Utilities for everyday stuff.
 * 
 * Higly inspired (copy) by : https://github.com/playframework/play/blob/master/framework/src/play/libs/F.java
 *
 * @author Mathieu ANCELIN
 */
public class F {

    final static None<Object> None = new None<Object>();

    public static abstract class Option<T> implements Iterable<T> {

        public abstract boolean isDefined();

        public abstract T get();

        public abstract T getOrElse(T value);

        public static <T> None<T> None() {
            return (None<T>) (Object) None;
        }

        public static <T> Some<T> Some(T value) {
            return new Some<T>(value);
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

    public static class Either<A, B> {

        final public Option<A> _1;
        final public Option<B> _2;

        private Either(Option<A> _1, Option<B> _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public static <A, B> Either<A, B> _1(A value) {
            return new Either(some(value), None);
        }

        public static <A, B> Either<A, B> _2(B value) {
            return new Either(None, some(value));
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
    
    public static <A, B> Tuple<A, B> tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static <A> Some<A> some(A a) {
        return new Some(a);
    }
}