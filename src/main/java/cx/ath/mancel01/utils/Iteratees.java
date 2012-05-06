package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.actors.Actors;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Iteratees {
    
    public static final class Elem<I> {
        private final I e;
        public Elem(I e) { this.e = e; }
        public Option<I> get() { return Option.apply(e); }
    }
    public static enum EOF { INSTANCE }
    public static enum Empty { INSTANCE }
    private static enum Run { INSTANCE }
    public static enum Done { INSTANCE }
    public static enum Cont { INSTANCE }
    public static final class Error<E> {
        public final E error;
        public Error(E error) {
            this.error = error;
        }
    }
    
    public static abstract class Iteratee<I, O> implements Behavior {
        protected Promise<O> promise = new Promise<O>();
        public Effect done(O result, Context ctx) {
            promise.apply(result);
            ctx.from.tell(Done.INSTANCE, ctx.me);
            return Actors.DIE;
        }
        public Promise<O> getAsyncResult() {
            return promise;
        }
        public static <T> Iteratee<T, Unit> foreach(Function<T, Effect> func) {
            return new ForeachIteratee<T>(func);
        }
    }
    
    public static abstract class Enumerator<I> implements Behavior {
        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Run run : M.caseClassOf(Run.class, msg)) {
                sendNext(msg, ctx);
            }
            for (Cont cont : M.caseClassOf(Cont.class, msg)) {
                sendNext(msg, ctx);
            }
            for (Done done : M.caseClassOf(Done.class, msg)) {
                ctx.from.tell(Poison.PILL, ctx.me);
                return Actors.DIE;
            }
            for (Error err : M.caseClassOf(Error.class, msg)) {
                ctx.from.tell(Poison.PILL, ctx.me);
                System.err.println(err.error);
                return Actors.DIE;            
            }
            return Actors.CONTINUE;
        }
        void sendNext(Object msg, Context ctx) {
            if (!hasNext()) {
                ctx.from.tell(EOF.INSTANCE, ctx.me);
            } else {
                Option<I> optElemnt = next();
                for (I element : optElemnt) {
                    ctx.from.tell(new Elem<I>(element), ctx.me);
                }
                if (optElemnt.isEmpty()) {
                    ctx.from.tell(Empty.INSTANCE, ctx.me);
                }
            }
        }
        public abstract boolean hasNext();
        public abstract Option<I> next();
        Actor enumerator;
        Actor iteratee;
        public <O> Promise<O> applyOn(Iteratee<I, O> it) {
            Promise<O> res = it.getAsyncResult();
            ActorContext context = Actors.newContext();
            enumerator = context.create(this, UUID.randomUUID().toString());
            iteratee = context.create(it, UUID.randomUUID().toString());
            enumerator.tell(Run.INSTANCE, iteratee);
            return res;
        }
        
        public static <T> Enumerator of(T... args) {
            return new IterableEnumerator(Arrays.asList(args));
        }
        
        public static <T> Enumerator of(Iterable<T> iterable) {
            return new IterableEnumerator(iterable);
        }
        public static <T> Enumerator fromStream(InputStream is, int chunkSize) {
            return new FromInputStreamEnumerator(is, chunkSize);
        }
        public static <T> Enumerator fromFile(File f, int chunkSize) {
            try {
                return new FromInputStreamEnumerator(new FileInputStream(f), chunkSize);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        public static <T> Enumerator fromFileLines(File f) {
            return new FromFileLinesEnumerator(f);
        }
        public static <T> PushEnumerator<T> push(Class<T> clazz) {
            return new PushEnumerator<T>();
        }
        public static <T> PushEnumerator<T> fromCallback(long every, TimeUnit unit, final Function<Unit, Option<T>> callback) {
            final PushEnumerator<T> pushEnum = new PushEnumerator<T>();
            Actors.newContext().schedule(every, unit, new Runnable() {
                @Override
                public void run() {
                    Option<T> opt = callback.apply(Unit.unit());
                    for (T elem : opt) {
                        pushEnum.push(elem);
                    }
                }
            });
            return pushEnum;
        }
    }   
    
    public static class IterableEnumerator<T> extends Enumerator<T> {
        private final Iterator<T> it;
        public IterableEnumerator(Iterable<T> iterable) {
            it = iterable.iterator();
        }
        @Override
        public Option<T> next() {
            T obj = null;
            try {
                obj = it.next();
            } catch (Exception e) { e.printStackTrace(); }
            return Option.apply(obj);
        }
        @Override
        public boolean hasNext() {
            return it.hasNext();
        }
    }
    public static class LongEnumerator extends Enumerator<Long> {
        private Long current = 0L;
        @Override
        public Option<Long> next() {
            current = current + 1L;
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Long.MAX_VALUE);
        }
    }
    public static class IntEnumerator extends Enumerator<Integer> {
        private Integer current = 0;
        @Override
        public Option<Integer> next() {
            current = current + 1;
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Integer.MAX_VALUE);
        }
    }
    public static class ShortEnumerator extends Enumerator<Short> {
        private Short current = 0;
        @Override
        public Option<Short> next() {
            current = (short)(current + 1);
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Short.MAX_VALUE);
        }
    }
    public static class FloatEnumerator extends Enumerator<Float> {
        private Float current = 0F;
        @Override
        public Option<Float> next() {
            current = current + 1F;
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Float.MAX_VALUE);
        }
    }
    public static class DoubleEnumerator extends Enumerator<Double> {
        private Double current = 0.0;
        @Override
        public Option<Double> next() {
            current = current + 1.0;
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Double.MAX_VALUE);
        }
    }
    public static class ByteEnumerator extends Enumerator<Byte> {
        private Byte current = 0;
        @Override
        public Option<Byte> next() {
            current = (byte)(current + 1);
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current < Byte.MAX_VALUE);
        }
    }
    public static class BigIntegerEnumerator extends Enumerator<BigInteger> {
        private BigInteger current = BigInteger.ZERO;
        @Override
        public Option<BigInteger> next() {
            current = current.add(BigInteger.ONE);
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) == -1);
        }
    }
    public static class BigDecimalEnumerator extends Enumerator<BigDecimal> {
        private BigDecimal current = BigDecimal.ZERO;
        @Override
        public Option<BigDecimal> next() {
            current = current.add(BigDecimal.ONE);
            return Option.some(current);
        }
        @Override
        public boolean hasNext() {
            return (current.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) == -1);
        }
    }
    public static class CharacterEnumerator extends Enumerator<Character> {
        private int current = 0;
        @Override
        public Option<Character> next() {
            current = current + 1;
            return Option.some(new Character(((char)current)));
        }
        @Override
        public boolean hasNext() {
            return (current < Character.MAX_VALUE);
        }
    }
    public static class FromInputStreamEnumerator extends Enumerator<Byte[]> {
        private final InputStream is;
        private final int chunkSize;
        private boolean hasnext = true;
        public FromInputStreamEnumerator(InputStream is, int chunkSize) {
            this.is = is;
            this.chunkSize = chunkSize;
        }
        @Override
        public Option<Byte[]> next() {
            byte[] bytes = new byte[chunkSize];
            Byte[] copy = new Byte[chunkSize];
            try {    
                int numRead = is.read(bytes);
                if (numRead == -1) {
                    close();
                } else {
                    System.arraycopy(bytes, 0, copy, 0, numRead);
                }
            } catch (Exception e) { 
                e.printStackTrace(); 
                close();
            } 
            return Option.some(copy);
        }
        private void close() {
            hasnext = false;
            try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        @Override
        public boolean hasNext() {
            return hasnext;
        }
    }
    public static class FromFileLinesEnumerator extends Enumerator<String> {
        private final FileInputStream fstream;
        private final DataInputStream in;
        private final BufferedReader br;
        private boolean hasnext = true;
        public FromFileLinesEnumerator(File f) {
            try {
                fstream = new FileInputStream(f);
                in = new DataInputStream(fstream);
                br = new BufferedReader(new InputStreamReader(in));
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public Option<String> next() {
            String strLine = null;
            try {
                br.readLine();
            } catch(Exception e) { e.printStackTrace(); }
            if (strLine == null) {
                close();
            }
            return Option.some(strLine);
        }
        private void close() {
            hasnext = false;
            try {
                br.close();
                in.close();
                fstream.close();
            } catch(Exception e) { e.printStackTrace(); }
        }
        @Override
        public boolean hasNext() {
            return hasnext;
        }
    }
    public static class PushEnumerator<T> extends Enumerator<T> {
        private boolean hasnext = true;
        private final ConcurrentLinkedQueue<T> pushQueue = new ConcurrentLinkedQueue<T>();
        @Override
        public Option<T> next() {
            return Option.apply(pushQueue.poll());
        }
        @Override
        public boolean hasNext() {
            if (!pushQueue.isEmpty()) {
                return true;
            }
            return hasnext;
        }
        public void push(T elem) {
            pushQueue.offer(elem);
            Option<T> optElemnt = next();
            for (T element : optElemnt) {
                iteratee.tell(new Elem<T>(element), enumerator);
            }
            if (optElemnt.isEmpty()) {
                iteratee.tell(Empty.INSTANCE, enumerator);
            }
        }
        public void stop() {
            hasnext = false;
            iteratee.tell(EOF.INSTANCE, enumerator);
            enumerator.tell(Done.INSTANCE, iteratee);
        }
    } 
    public static class ForeachIteratee<T> extends Iteratee<T, Unit> {
        private final Function<T, Effect> func;
        public ForeachIteratee(Function<T, Effect> func) {
            this.func = func;
        }
        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Elem e : M.caseClassOf(Elem.class, msg)) {
                Elem<T> el = (Elem<T>) e;
                for (T elem : el.get()) { 
                   Effect effect = func.apply(elem);
                   if (effect.equals(Actors.DIE)) {
                       return done(Unit.unit(), ctx);
                   }
                }
                ctx.from.tell(Cont.INSTANCE, ctx.me);
            }
            for (EOF e : M.caseClassOf(EOF.class, msg)) {
                return done(Unit.unit(), ctx);
            }
            return Actors.CONTINUE;
        }
    }
}
