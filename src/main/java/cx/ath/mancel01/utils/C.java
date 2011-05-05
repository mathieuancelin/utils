package cx.ath.mancel01.utils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utilities for collections (inspired by Google Collections)
 *
 * @author Mathieu ANCELIN
 */
public class C {

    public static <T> Each<T> forEach(Collection<T> collection) {
        return new EachImpl<T>(collection);
    }

    public static <T> Collection<T> filtered(Collection<T> collection, Predicate<T> predicate) {
        return new EachImpl<T>(collection).filteredBy(predicate).get();
    }

    public static <T> Filterable<T> filter(Collection<T> collection, Predicate<T> predicate) {
        return new EachImpl<T>(collection).filteredBy(predicate);
    }

    public static <T> Joiner join(Collection<T> collection) {
        return new JoinerImpl(collection);
    }

    public static Predicate<String> eq(final String value) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String t) {
                return value.equals(t);
            }
        };
    }
    public static Predicate<Integer> eq(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value == t.intValue();
            }
        };
    }
    public static Predicate<Long> eq(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value == t.longValue();
            }
        };
    }
    public static Predicate<Object> eq(final Object value) {
        return new Predicate<Object>() {
            @Override
            public boolean apply(Object t) {
                return value.equals(t);
            }
        };
    }
    public static Predicate<String> notEq(final String value) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String t) {
                return !value.equals(t);
            }
        };
    }
    public static Predicate<Long> notEq(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value != t.longValue();
            }
        };
    }
    public static Predicate<Integer> notEq(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value != t.intValue();
            }
        };
    }
    public static Predicate<Object> notEq(final Object value) {
        return new Predicate<Object>() {
            @Override
            public boolean apply(Object t) {
                return value.equals(t);
            }
        };
    }
    public static Predicate<Object> isNull(final Object value) {
        return new Predicate<Object>() {
            @Override
            public boolean apply(Object t) {
                return value == null;
            }
        };
    }
    public static Predicate<Object> notNull(final Object value) {
        return new Predicate<Object>() {
            @Override
            public boolean apply(Object t) {
                return !(value == null);
            }
        };
    }
    public static Predicate<String> matchRegex(final String regexp) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String t) {
                return regexp.matches(t);
            }
        };
    }
    public static Predicate<Integer> greaterThan(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value < t.intValue();
            }
        };
    }
    public static Predicate<Long> greaterThan(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value < t.longValue();
            }
        };
    }
    public static Predicate<Integer> lesserThan(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value > t.intValue();
            }
        };
    }
    public static Predicate<Long> lesserThan(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value > t.longValue();
            }
        };
    }
    public static Predicate<Integer> greaterEqThan(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value <= t.intValue();
            }
        };
    }
    public static Predicate<Long> greaterEqThan(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value <= t.longValue();
            }
        };
    }
    public static Predicate<Integer> lesserEqThan(final int value) {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(Integer t) {
                return value >= t.intValue();
            }
        };
    }
    public static Predicate<Long> lesserEqThan(final long value) {
        return new Predicate<Long>() {
            @Override
            public boolean apply(Long t) {
                return value >= t.longValue();
            }
        };
    }

    public static interface Each<T> extends Filterable<T> {

        @Override
        Each<T> filteredBy(Predicate<T> predicate);

        Filterable<T> execute(Function<T> action);

        <R> Filterable<R> apply(Transformation<T, R> transformation);
    }

    public static interface Joiner {

        <T> Joiner labelized(Transformation<T, String> tranformation);

        Joiner before(String before);

        Joiner after(String after);

        String with(String separator);
    }

    public static interface Predicate<T> {

        boolean apply(T t);
    }

    public static interface Function<T> {

        void apply(T t);
    }

    public static interface Transformation<T, R> {

        R apply(T t);
    }

    public static interface Filterable<T> {
        
        Filterable<T> filteredBy(Predicate<T> predicate);

        Collection<T> get();

        int count();

        boolean isEmpty();

    }

    private static class JoinerImpl implements Function, Joiner {

        private String separator;

        private final StringBuilder builder = new StringBuilder();

        private final Collection<?> value;

        private String before;

        private String after;

        private Transformation label = new Transformation<Object, String>() {

            @Override
            public String apply(Object t) {
                return t.toString();
            }
        };

        public JoinerImpl(Collection<?> value) {
            this.value = value;
        }

        @Override
        public void apply(Object t) {
            builder.append(label.apply(t));
            builder.append(separator);
        }

        @Override
        public String with(String separator) {
            this.separator = separator;
            if (before != null) {
                builder.append(before);
            }
            forEach(value).execute((Function) this);
            if (before != null) {
                builder.append(after);
            }
            String finalValue = builder.toString();
            return finalValue.substring(0, finalValue.lastIndexOf(separator))
                    + finalValue.substring(finalValue.lastIndexOf(separator) + separator.length());
        }

        @Override
        public <R> Joiner labelized(Transformation<R, String> tranformation) {
            this.label = tranformation;
            return this;
        }

        @Override
        public Joiner before(String before) {
            this.before = before;
            return this;
        }

        @Override
        public Joiner after(String after) {
            this.after = after;
            return this;
        }
    }

    private static class EachImpl<T> implements Each<T> {

        private final Collection<T> baseCollection;
        
        private Collection<T> workingCollection;

        public EachImpl(Collection<T> baseCollection) {
            this.baseCollection = baseCollection;
             initWorkingCollection();
        }

        @Override
        public Each<T> filteredBy(Predicate<T> predicate) {
            initWorkingCollection();
            Collection<T> tmp = new ArrayList<T>();
            for (T element : workingCollection) {
                if (!predicate.apply(element)) {
                    tmp.add(element);
                }
            }
            for (T element : tmp) {
                workingCollection.remove(element);
            }
            return this;
        }

        private void initWorkingCollection() {
            if (workingCollection == null) {
                workingCollection = new ArrayList<T>();
                for (T element : baseCollection) {
                    workingCollection.add(element);
                }
            }
        }

        @Override
        public Filterable<T> execute(Function<T> action) {
            initWorkingCollection();
            for (T element : workingCollection) {
                action.apply(element);
            }
            return this;
        }

        @Override
        public <R> Filterable<R> apply(Transformation<T, R> transformation) {
            initWorkingCollection();
            Collection<R> tmp = new ArrayList<R>();
            for (T element : workingCollection) {
                tmp.add(transformation.apply(element));
            }
            return new EachImpl<R>(tmp);
        }

        @Override
        public Collection<T> get() {
            return workingCollection;
        }

        @Override
        public int count() {
            return workingCollection.size();
        }

        @Override
        public boolean isEmpty() {
            return workingCollection.isEmpty();
        }
    }
}
