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

import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.CheckedAction;
import cx.ath.mancel01.utils.F.CheckedFunction;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Tuple;
import java.util.Collections;
import java.util.Iterator;

public class Monads {
    
    public static interface MRFunction<T, R> {

        Tuple<T, R> apply(Monad<T, ?> monad) throws Throwable;
    }

    public static interface MFunction<T, R> {

        R apply(Monad<T, ?> monad) throws Throwable;
    }

    public static interface Monad<T, R>  extends Iterable<T> {

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

    public static class Monadic<T, R> implements Monad<T, R> {

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
                return Option.maybe(get());
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
