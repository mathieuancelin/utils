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
    
    public static <A, B> Tuple<A, B> tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static <A> Some<A> some(A a) {
        return new Some(a);
    }

    public static <A> Maybe<A> maybe(A a) {
        return new Maybe(a);
    }

    public static abstract class Matcher<T, R> {

        public abstract Option<R> match(T o);

        public Option<R> match(Option<T> o) {
            if (o.isDefined()) {
                return match(o.get());
            }
            return Option.none();
        }

        public <NR> Matcher<T, NR> and(final Matcher<R, NR> nextMatcher) {
            final Matcher<T, R> firstMatcher = this;
            return new Matcher<T, NR>() {

                @Override
                public Option<NR> match(T o) {
                    for (R r : firstMatcher.match(o)) {
                        return nextMatcher.match(r);
                    }
                    return Option.none();
                }
            };
        }
        public static Matcher<Object, String> string = new Matcher<Object, String>() {

            @Override
            public Option<String> match(Object o) {
                if (o instanceof String) {
                    return Option.some((String) o);
                }
                return Option.none();
            }
        };

        public static <K> Matcher<Object, K> ClassOf(final Class<K> clazz) {
            return new Matcher<Object, K>() {

                @Override
                public Option<K> match(Object o) {
                    if (o instanceof Option && ((Option) o).isDefined()) {
                        o = ((Option) o).get();
                    }
                    if (clazz.isInstance(o)) {
                        return Option.some((K) o);
                    }
                    return Option.none();
                }
            };
        }

        public static Matcher<String, String> StartsWith(final String prefix) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.startsWith(prefix)) {
                        return Option.some(o);
                    }
                    return Option.none();
                }
            };
        }

        public static Matcher<String, String> Re(final String pattern) {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o.matches(pattern)) {
                        return Option.some(o);
                    }
                    return Option.none();
                }
            };
        }

        public static <X> Matcher<X, X> Equals(final X other) {
            return new Matcher<X, X>() {

                @Override
                public Option<X> match(X o) {
                    if (o.equals(other)) {
                        return Option.some(o);
                    }
                    return Option.none();
                }
            };
        }

        public static Matcher<String, String> All() {
            return new Matcher<String, String>() {

                @Override
                public Option<String> match(String o) {
                    if (o != null) {
                        return Option.some(o);
                    }
                    return Option.none();
                }
            };
        }
    }

    public static void truc() {
        String value = "";
        for (String s : Matcher.string
                .and(Matcher.StartsWith("Foo"))
                .and(Matcher.Equals(""))
                .and(Matcher.All())
                .match(value)) {
           value = s.toUpperCase();
        }
    }
}