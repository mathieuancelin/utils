package cx.ath.mancel01.utils.actors;

import static cx.ath.mancel01.utils.M.*;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.Assert;
import org.junit.Test;

public class PingPongLocalTest {
    
    public static final CountDownLatch latch = new CountDownLatch(200);
    
    public static final ExecutorService service = Executors.newCachedThreadPool();
    
    public static class Run {    }
        
    @Test
    public void actorTest() throws Exception {
        ActorContext ctx = Actors.newContext();
        Actor pong = ctx.create(PONG, "pong", service);
        Actor ping = ctx.create(PING, "ping", service);
        ping.tell(new Run());
        latch.await();
        Assert.assertTrue(latch.getCount() <= 0);
        ping.tell(Poison.PILL);
        pong.tell(Poison.PILL);
    }
    
    public static final Behavior PING = new Behavior() {
        
        @Override
        public Effect apply(Object t, Context ctx) {
            if (latch.getCount() <= 0) {
                return Actors.DIE;
            }
            for (Run run : caseClassOf(Run.class, t)) {
                ctx.lookup("pong").tell("PING", ctx.me);
            }
            for (String s : caseStringEquals(t, "PONG")) {
                latch.countDown();
                System.out.println("Received PONG : " + latch.getCount());
                ctx.from.tell("PING", ctx.me);
            }
            return Actors.CONTINUE;
        }
    };
    
    public static final Behavior PONG = new Behavior() {
        
        @Override
        public Effect apply(Object t, Context ctx) {
            if (latch.getCount() <= 0) {
                return Actors.DIE;
            }
            for (String s : caseStringEquals(t, "PING")) {
                latch.countDown();
                System.out.println("Received PING : " + latch.getCount());
                ctx.from.tell("PONG", ctx.me);
            }
            return Actors.CONTINUE;
        }
    };
}
