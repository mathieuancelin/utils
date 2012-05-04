package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.Assert;
import org.junit.Test;

public class AsyncTest {
    
    public static final CountDownLatch latch = new CountDownLatch(199);
    
    public static final ExecutorService service = Executors.newCachedThreadPool();
    
    public static final class Run{}
        
    @Test
    public void queueTest() throws Exception {
        final Actor consumer = Actors.newContext().create(CONSUME, "CONSUME", service);
        for (int i = 0; i < 4; i++) {
            System.out.println("[PRODUCER] PING");
            consumer.tell(new Run());
            System.out.println("[PRODUCER] ZZZZZzzzzz");
            Thread.sleep(1000);
        }
        Assert.assertEquals(latch.getCount(), 0);
        consumer.tell(Poison.PILL);
    }
    
    public static final Behavior CONSUME = new Behavior() {
        
        @Override
        public Effect apply(Object t, Actors.Context ctx) {
            for (Run run : M.caseClassOf(Run.class, t)) {
                for (int i = 0; i < 50; i++) {
                    System.out.println("[CONSUME] eating " + latch.getCount());
                    latch.countDown();
                }
            }
            if (latch.getCount() == 0) {
                return Actors.DIE;
            }      
            return Actors.CONTINUE;
        }
    };
}
