package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.AsyncIteratees.Cont;
import cx.ath.mancel01.utils.actors.AsyncIteratees.EOF;
import cx.ath.mancel01.utils.actors.AsyncIteratees.Elem;
import cx.ath.mancel01.utils.actors.AsyncIteratees.Enumerator;
import cx.ath.mancel01.utils.actors.AsyncIteratees.Iteratee;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

public class AsyncIterateeTest {
    
    @Test
    public void testIterateeOnList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        List<String> list = Arrays.asList(new String[]{"Mathieu", "Kevin", "Jeremy"});
        Promise<String> promise = new ListEnumerator<String>(list)
                    .applyOn(new ListIteratee());
        promise.onRedeem(new F.Action<Promise<String>>() {
            @Override
            public void apply(Promise<String> t) {
                try {
                    System.out.println(t.get());
                    Assert.assertEquals(t.get(), "MathieuKevinJeremy");
                    latch.countDown();
                } catch (Exception ex) {}
            }
        });  
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }
    
    @Test
    public void testIterateeOnLong() throws Exception {
        final CountDownLatch latch = new CountDownLatch(501);
        Promise<Unit> promise = new LongEnumerator().applyOn(new LongIteratee(latch));
        promise.onRedeem(new F.Action<Promise<Unit>>() {
            @Override
            public void apply(Promise<Unit> t) {
                latch.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
    }

    public static class ListIteratee extends Iteratee<String, String> {
        
        private StringBuilder builder = new StringBuilder();

        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Elem e : M.caseClassOf(Elem.class, msg)) {
                Elem<String> el = (Elem<String>) e;
                for (String s : el.get()) {
                    builder.append(s);
                }
                ctx.from.tell(Cont.INSTANCE, ctx.me);
            }
            for (EOF e : M.caseClassOf(EOF.class, msg)) {
                return done(builder.toString(), ctx);
            }
            return Actors.CONTINUE;
        }
    }
    
    public static class LongIteratee extends Iteratee<Long, Unit> {
        
        private final CountDownLatch latch;

        public LongIteratee(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Elem e : M.caseClassOf(Elem.class, msg)) {
                Elem<Long> el = (Elem<Long>) e;
                for (Long l : el.get()) {
                    if (l > 500) {
                        return done(Unit.unit(), ctx);
                    }
                    System.out.println(l);
                    latch.countDown();
                }
                ctx.from.tell(Cont.INSTANCE, ctx.me);
            }
            return Actors.CONTINUE;
        }
    }
    
    public static class ListEnumerator<T> extends Enumerator<T> {
        private final Iterator<T> it;
        public ListEnumerator(List<T> names) {
            it = names.iterator();
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
}
