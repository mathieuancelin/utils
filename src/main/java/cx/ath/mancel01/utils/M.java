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

import cx.ath.mancel01.utils.F.Option;

/**
 * Pattern matching for Java (with more or less verbose).
 *
 * @author Mathieu ANCELIN
 */
public final class M {

    private M() {}

    public static <T> UnexpectedMatch<T> match(final T value) {
        return new UnexpectedMatch<T>() {

            @Override
            public <R> ExpectedMatch<T, R> andExpect(Class<R> returnType) {
                return new ExpectedMatchImpl<T, R>(value);
            }

            @Override
            public Option<Object> with(ExecutableMatcher<T, Object>... matchers) {
                return new ExpectedMatchImpl<T, Object>(value).with(matchers);
            }
        };
    }

    public static interface ExecutableMatcher<T, R> {

        Option<R> execute(T value);
    }

    public static interface MatchCaseFunction<T, R> {

        Option<R> apply(T value);
    }

    public static interface UnexpectedMatch<T> extends ExpectedMatch<T, Object> {

        <R> ExpectedMatch<T, R> andExpect(Class<R> returnType);
    }

    public static interface ExpectedMatch<T, R> {

        Option<R> with(ExecutableMatcher<T, R>... matchers);
    }

    public static <T, R> Matcher<T, R> with(Matcher<T, R> matcher) {
        return matcher;
    }

    public static abstract class Matcher<T, R> {

        MatchCaseFunction<T, R> function;

        public abstract Option<R> match(T o);

        public <NR> Matcher<T, NR> and(final Matcher<R, NR> next) {
            final Matcher<T, R> first = this;
            return new Matcher<T, NR>() {
                @Override
                public Option<NR> match(T o) {
                    for (R r : first.match(o)) {
                        return next.match(r);
                    }
                    return Option.none();
                }
            };
        }

        public ExecutableMatcher<T, R> then(MatchCaseFunction<T, R> function) {
            this.function = function;
            return new ExecutableMatcherImpl<T, R>(this);
        }
    }

    private static class ExecutableMatcherImpl<T, R> implements ExecutableMatcher<T, R> {

        private final Matcher<T, R> matcher;

        public ExecutableMatcherImpl(Matcher<T, R> matcher) {
            this.matcher = matcher;
        }

        @Override
        public Option<R> execute(T value) {
            Option<R> matched = matcher.match(value);
            if (matched.isDefined()) {
                return matcher.function.apply(value);
            } else {
                return Option.none();
            }
        }
    }

    private static class ExpectedMatchImpl<T, R> implements ExpectedMatch<T, R> {

        private final T value;

        public ExpectedMatchImpl(T value) {
            this.value = value;
        }

        @Override
        public Option<R> with(ExecutableMatcher<T, R>... matchers) {
            for (ExecutableMatcher matcher : matchers) {
                Option<R> option = matcher.execute(value);
                if (option.isDefined()) {
                    return option;
                }
            }
            return Option.none();
        }
    }

    public static <K> Matcher<Object, K> caseClassOf(final Class<K> clazz) {
        return new Matcher<Object, K>() {

            @Override
            public Option<K> match(Object o) {
                if (clazz.isInstance(o)) {
                    return Option.some(clazz.cast(o));
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseStartsWith(final String prefix) {
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

    public static Matcher<String, String> caseLengthGreater(final int than) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.length() > than) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseLengthGreaterEq(final int than) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.length() >= than) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseLengthLesser(final int than) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.length() < than) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseLengthLesserEq(final int than) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.length() <= than) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseLengthEquals(final int with) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.length() == with) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static Matcher<String, String> caseRegex(final String pattern) {
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

    public static <X> Matcher<X, X> caseEquals(final X other) {
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

    public static Matcher<String, String> caseContains(final String contain) {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                if (o.contains(contain)) {
                    return Option.some(o);
                }
                return Option.none();
            }
        };
    }

    public static <X> Matcher<String, String> otherCases() {
        return new Matcher<String, String>() {

            @Override
            public Option<String> match(String o) {
                return Option.some(o);
            }
        };
    }
    
    public static <K> Option<K> caseClassOf(final Class<K> clazz, Object o) {
        if (clazz.isInstance(o)) {
            return Option.some(clazz.cast(o));
        }
        return Option.none();
    }
    
    public static <K> Option<K> caseObjEquals(Object a, K b) {
        if (a.equals(b)) {
            return Option.some(b);
        }
        return Option.none();
    }
    
    public static Option<String> caseStringEquals(Object o, String value) {
        for (String s : caseClassOf(String.class, o)) {
            if (s.equals(value)) {
                return Option.some(s);
            }
        }
        return Option.none();
    }
    
    public static Option<String> caseStringMatch(Object o, String regex) {
        for (String s : caseClassOf(String.class, o)) {
            if (s.matches(regex)) {
                return Option.some(s);
            }
        }
        return Option.none();
    }
    
    public static Option<String> caseStringContains(Object o, String value) {
        for (String s : caseClassOf(String.class, o)) {
            if (s.contains(value)) {
                return Option.some(s);
            }
        }
        return Option.none();
    }
    
    public static Option<String> caseStringStartWith(Object o, String value) {
        for (String s : caseClassOf(String.class, o)) {
            if (s.startsWith(value)) {
                return Option.some(s);
            }
        }
        return Option.none();
    }
}
