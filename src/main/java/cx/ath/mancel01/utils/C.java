package cx.ath.mancel01.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Utilities for collections (inspired by Google Collections)
 *
 * @author Mathieu ANCELIN
 */
public class C {

    /**
     * Return an object capable of applying a function for each item in the collection.
     *
     * @param <T>
     * @param collection the processed collection
     * @return an Each Object
     */
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

        @Override
        Each<T> parFilteredBy(Predicate<T> predicate);

        Filterable<T> execute(Function<T> action);

        <R> Filterable<R> apply(Transformation<T, R> transformation);

        Filterable<T> parExecute(Function<T> action);

        <R> Filterable<R> parApply(Transformation<T, R> transformation);
    }

    public static interface Joiner {

        <T> Joiner labelized(Transformation<T, String> tranformation);

        Joiner before(String before);

        Joiner after(String after);

        String with(String separator);

        String parWith(String separator);
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

        Filterable<T> parFilteredBy(Predicate<T> predicate);

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
        public String parWith(String separator) {
            this.separator = separator;
            if (before != null) {
                builder.append(before);
            }
            forEach(value).parExecute((Function) this);
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

        private static final int NBR_CORE =
                Runtime.getRuntime().availableProcessors() + 1;

        private final ExecutorService executor = 
            Executors.newFixedThreadPool(NBR_CORE);

        private final Collection<T> baseCollection;
        
        private List<T> workingCollection;

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


        private Collection<Bound> getBulkBounds() {
            Collection<Bound> bulksCollections = new ArrayList<Bound>();
            int bulkCollectionSize = (workingCollection.size() / NBR_CORE);
            int fromIndex = 0;
            boolean again = true;
            while (again) {
                if (fromIndex > workingCollection.size()) {
                    break;
                }
                int toIndex = (fromIndex - 1) + bulkCollectionSize;
                if (toIndex > workingCollection.size()) {
                    toIndex = workingCollection.size();
                    again = false;
                }
                bulksCollections.add(new Bound(fromIndex, toIndex));
                fromIndex = toIndex + 1;
            }
            return bulksCollections;
        }

        @Override
        public Filterable<T> parExecute(final Function<T> action) {
            Collection<Future<Void>> bulkExecutions = new ArrayList<Future<Void>>();
            initWorkingCollection();
            Collection<Bound> bulkBounds = getBulkBounds();
            for (Bound bound : bulkBounds) {
                bulkExecutions.add(
                    executor.submit(
                        new BulkExecution<T>(
                            action, workingCollection, bound)));
            }
            for (Future<Void> f : bulkExecutions) {
                try {
                    f.get();
                } catch (Exception ex) {
                    //
                }
            }
            return this;
        }

        @Override
        public <R> Filterable<R> parApply(final Transformation<T, R> transformation) {
            Collection<R> tmp = new ArrayList<R>();
            Collection<Future<Collection<R>>> bulkExecutions = new ArrayList<Future<Collection<R>>>();
            initWorkingCollection();
            Collection<Bound> bulkBounds = getBulkBounds();
            for (Bound bound : bulkBounds) {
                bulkExecutions.add(
                    executor.submit(
                        new BulkTransformation<T, R>(
                            transformation, workingCollection, bound)));
            }
            for (Future<Collection<R>> f : bulkExecutions) {
                try {
                    tmp.addAll(f.get());
                } catch (Exception ex) {
                    //
                }
            }
            return new EachImpl<R>(tmp);
        }

        @Override
        public Each<T> parFilteredBy(Predicate<T> predicate) {
            Collection<T> tmp = new ArrayList<T>();
            Collection<Future<Collection<T>>> bulkExecutions = 
                    new ArrayList<Future<Collection<T>>>();
            initWorkingCollection();
            Collection<Bound> bulkBounds = getBulkBounds();
            for (Bound bound : bulkBounds) {
                bulkExecutions.add(
                    executor.submit(
                        new BulkFilter<T>(
                            predicate, workingCollection, bound)));
            }
            for (Future<Collection<T>> f : bulkExecutions) {
                try {
                    tmp.addAll(f.get());
                } catch (Exception ex) {
                    //
                }
            }
            for (T element : tmp) {
                workingCollection.remove(element);
            }
            return this;
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

    private static class BulkFilter<T> implements Callable<Collection<T>> {

        private final Predicate<T> predicate;
        private final List<T> collection;
        private final Bound bound;

        public BulkFilter(Predicate<T> predicate, List<T> collection, Bound bound) {
            this.predicate = predicate;
            this.collection = collection;
            this.bound = bound;
        }

        @Override
        public Collection<T> call() throws Exception {

            Collection<T> tmp = new ArrayList<T>();
            for (int i = bound.fromIndex; i < (bound.toIndex + 1); i++) {
                T element = collection.get(i);
                if (!predicate.apply(element)) {
                    tmp.add(element);
                }
            }
            return tmp;
        }
    }

    private static class BulkExecution<T> implements Callable<Void> {

        private final Function<T> action;
        private final List<T> collection;
        private final Bound bound;

        public BulkExecution(Function<T> action, List<T> collection, Bound bound) {
            this.action = action;
            this.collection = collection;
            this.bound = bound;
        }

        @Override
        public Void call() throws Exception {
            for (int i = bound.fromIndex; i < (bound.toIndex + 1); i++) {
                action.apply(collection.get(i));
            }
            return null;
        }
    }

    private static class BulkTransformation<T, R> implements Callable<Collection<R>> {

        private final Transformation<T, R> transfo;
        private final List<T> collection;
        private final Bound bound;

        public BulkTransformation(Transformation<T, R> transfo, List<T> collection, Bound bound) {
            this.transfo = transfo;
            this.collection = collection;
            this.bound = bound;
        }

        @Override
        public Collection<R> call() throws Exception {
            Collection<R> tmp = new ArrayList<R>();
            for (int i = bound.fromIndex; i < (bound.toIndex + 1); i++) {
                tmp.add(transfo.apply(collection.get(i)));
            }
            return tmp;
        }
    }

    private static class Bound {
        final int fromIndex;
        final int toIndex;

        public Bound(int fromIndex, int toIndex) {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public String toString() {
            return "Bound [ from=" + fromIndex + ", to=" + toIndex + " ]";
        }
    }
}
