package cx.ath.mancel01.utils.actors;

import static cx.ath.mancel01.utils.M.*;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Test;

public class ScheduledTest {
    
    public static final CountDownLatch latch = new CountDownLatch(25);
    public static final CountDownLatch latch2 = new CountDownLatch(1);

    @Test
    public void testSchedule() throws Exception {
        ActorContext ctx = Actors.newContext();
        Actor sched = ctx.create(SCHEDULED, "SCHEDULED");
        ctx.scheduleOnce(3, TimeUnit.SECONDS, sched, Once.INSTANCE);
        ctx.schedule(200, TimeUnit.MILLISECONDS, sched, Tick.INSTANCE);
        latch2.await();
        latch.await();
        Assert.assertEquals(latch.getCount(), 0);
        Assert.assertEquals(latch2.getCount(), 0);
        sched.tell(Poison.PILL);
    }
    
    public static enum Tick { INSTANCE }
    
    public static enum Once { INSTANCE }
    
    public static final Actors.Behavior SCHEDULED = new Actors.Behavior() {
        
        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            if (latch.getCount() == 0) {
                return Actors.DIE;
            }
            for (Tick s : caseClassOf(Tick.class, t)) {
                System.out.println(new Date());
                latch.countDown();
            }
            for (Once s : caseClassOf(Once.class, t)) {
                System.out.println("Once happened !!!");
                latch2.countDown();
            }
            return Actors.CONTINUE;
        }
    };
}
