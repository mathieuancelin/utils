package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Option;

/**
 * Pattern matching for Java.
 *
 * @author Mathieu ANCELIN
 */
public class M {

    public static <T> UnexpectedMatch<T> match(final T value) {
        return new UnexpectedMatch<T>() {

            @Override
            public <R> ExpectedMatch<T, R> andExcept(Class<R> returnType) {
                return new ExpectedMatchImpl<T, R>(returnType, value);
            }
        };
    }

    public static interface ExecutableMatcher<T, R> {

        Option<R> execute(T value);
    }

    public static interface MatchCaseFunction<T, R> {

        Option<R> apply(T value);
    }

    public static interface UnexpectedMatch<T> {

        <R> ExpectedMatch<T, R> andExcept(Class<R> returnType);
    }

    public static interface ExpectedMatch<T, R> {

        Option<R> with(ExecutableMatcher<T, R>... matchers);
    }

    public static abstract class Matcher<T, R> {

        MatchCaseFunction<T, R> function;

        public abstract boolean match(T o);

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
            if (matcher.match(value)) {
                return matcher.function.apply(value);
            } else {
                return Option.none();
            }
        }
    }

    private static class ExpectedMatchImpl<T, R> implements ExpectedMatch<T, R> {

        private final Class<R> returnType;
        private final T value;

        public ExpectedMatchImpl(Class<R> returnType, T value) {
            this.returnType = returnType;
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
            public boolean match(Object o) {
                if (clazz.isInstance(o)) {
                    return true;
                }
                return false;
            }
        };
    }

    public static Matcher<String, String> caseStartsWith(final String prefix) {
        return new Matcher<String, String>() {

            @Override
            public boolean match(String o) {
                return o.startsWith(prefix);
            }
        };
    }

    public static Matcher<String, String> caseRegex(final String pattern) {
        return new Matcher<String, String>() {

            @Override
            public boolean match(String o) {
                return o.matches(pattern);
            }
        };
    }

    public static <X> Matcher<X, X> caseEquals(final X other) {
        return new Matcher<X, X>() {

            @Override
            public boolean match(X o) {
                return o.equals(other);
            }
        };
    }

    public static Matcher<String, String> caseContains(final String contain) {
        return new Matcher<String, String>() {

            @Override
            public boolean match(String o) {
                return o.contains(contain);
            }
        };
    }

    public static <X> Matcher<String, String> otherCases() {
        return new Matcher<String, String>() {

            @Override
            public boolean match(String o) {
                return true;
            }
        };
    }
}
