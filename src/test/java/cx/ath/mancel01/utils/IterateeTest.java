package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.Iteratees.Cont;
import cx.ath.mancel01.utils.Iteratees.EOF;
import cx.ath.mancel01.utils.Iteratees.Elem;
import cx.ath.mancel01.utils.Iteratees.Enumerator;
import cx.ath.mancel01.utils.Iteratees.Iteratee;
import cx.ath.mancel01.utils.Iteratees.LongEnumerator;
import cx.ath.mancel01.utils.Iteratees.PushEnumerator;
import cx.ath.mancel01.utils.actors.Actors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

public class IterateeTest {
    
    @Test
    public void testIterateeOnList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Promise<String> promise = Enumerator.of("Mathieu", "Kevin", "Jeremy")
                    .applyOn(new ListIteratee());
        promise.onRedeem(new Action<Promise<String>>() {
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
        final CountDownLatch latch = new CountDownLatch(500);
        final CountDownLatch latch2 = new CountDownLatch(1);
        Promise<Unit> promise = new LongEnumerator().applyOn(Iteratee.foreach(new Function<Long, Effect>() {
            @Override
            public Effect apply(Long l) {
                if (l > 500) {
                    return Actors.DIE;
                } else {
                    latch.countDown();
                    System.out.println(l);
                    return Actors.CONTINUE;
                }
            }
        }));
        promise.onRedeem(new Action<Promise<Unit>>() {
            @Override
            public void apply(Promise<Unit> t) {
                latch2.countDown();
            }
        });
        latch.await(10, TimeUnit.SECONDS);
        latch2.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(0, latch2.getCount());
    }
    
    @Test
    public void testPushEnumerator() throws Exception {
        final CountDownLatch latch = new CountDownLatch(5);
        PushEnumerator<String> pushEnum = Enumerator.push(String.class);
        pushEnum.applyOn(Iteratee.foreach(new Function<String, Effect>() {
            @Override
            public Effect apply(String t) {
                System.out.println(t);
                latch.countDown();
                return Actors.CONTINUE;
            }
        }));
        pushEnum.push("Hello dude");
        Thread.sleep(1000);
        pushEnum.push("Hello dude");
        Thread.sleep(2000);
        pushEnum.push("Hello dude");
        Thread.sleep(500);
        pushEnum.push("Hello dude");
        Thread.sleep(1000);
        pushEnum.push("Hello dude");
        latch.await();
        pushEnum.stop();
        Assert.assertEquals(0, latch.getCount());
    }
    
    //@Test
    public void testPushEnumeratorSched() throws Exception {
        final CountDownLatch latch = new CountDownLatch(10);
        PushEnumerator<String> pushEnum = Enumerator.fromCallback(1, TimeUnit.SECONDS, 
                new Function<Unit, Option<String>>() {
            @Override
            public Option<String> apply(Unit t) {
                latch.countDown();
                return Option.some("Hello dude");
            } 
        });
        pushEnum.applyOn(Iteratee.foreach(new Function<String, Effect>() {
            @Override
            public Effect apply(String t) {
                System.out.println(t);
                latch.countDown();
                return Actors.CONTINUE;
            }
        }));
        latch.await(10, TimeUnit.SECONDS);
        pushEnum.stop();
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
}
