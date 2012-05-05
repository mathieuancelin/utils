package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;

// enumerator sends El, Empty and EOF
// iteratee sends Done, Error, Cont(handler)
public class Iteratees {
    
    public static interface Input<I> {
        Option<I> get();
    }    
    public static final class El<I> implements Input<I> {
        private final I e;
        public El(I e) {
            this.e = e;
        }
        @Override
        public Option<I> get() {
            return Option.apply(e);
        }
    }
    public static class EOF<I> implements Input<I> {
        @Override
        public Option<I> get() {
            return Option.none();
        }
    }
    public static class Empty<I> implements Input<I> {
        @Override
        public Option<I> get() {
            return Option.none();
        }
    }
    
    public static interface Output<I, O> {
        Option<Function<Input<I>,Output<I, O>>> get();
    }
    
    
    public static final class Done<I, O> implements Output<I, O> {
        
        private final O o;

        public Done(O o) {
            this.o = o;
        }
        
        public Option<O> result() {
            return Option.apply(o);
        }

        @Override
        public Option<Function<Input<I>, Output<I, O>>> get() {
            return Option.none();
        }
    }
    public static final class Error<I, O> implements Output<I, O> {
        @Override
        public Option<Function<Input<I>, Output<I, O>>> get() {
            return Option.none();
        }
    }
    public static final class Cont<I, O> implements Output<I, O> {
        private final Option<Function<Input<I>, Output<I, O>>> handler;
        public Cont(Function<Input<I>, Output<I, O>> handler) {
            this.handler = Option.apply(handler);
        }
        @Override
        public Option<Function<Input<I>, Output<I, O>>> get() {
            return handler;
        }
    }
    
    public static abstract class Iteratee<I,O> {
                        
        public abstract Function<Input<I>, Output<I, O>> handler();
        
        public abstract O get();
    }    
    
    public static abstract class Enumerator<I> {
                
        public <O> Option<O> applyOn(Iteratee<I, O> iteratee) {
            Function<Input<I>, Output<I, O>> handler = iteratee.handler();
            Output<I, O> out = new Cont<I, O>(handler);
            while(out.getClass().equals(Cont.class)) {
                out = handler.apply(next());
                if (out instanceof Cont) {
                    for (Function<Input<I>, Output<I, O>> h : 
                            ((Cont<I, O>) out).get()) {
                        handler = h;
                    }
                }
            }
            for (Done done : M.caseClassOf(Done.class, out)) {
                Done<I,O> d = (Done<I,O>) done;
                return d.result();
            }
            for (Empty error : M.caseClassOf(Empty.class, out)) {
                return Option.none();
            }
            throw new IterateeException("Something went wrong");
        }
        
        public abstract Input<I> next();
    }
    
    public static class IterateeException extends RuntimeException {
        public IterateeException() {}
        public IterateeException(String string) {
            super(string);
        }
        public IterateeException(Throwable thrwbl) {
            super(thrwbl);
        }
        public IterateeException(String string, Throwable thrwbl) {
            super(string, thrwbl);
        }
    }
}
