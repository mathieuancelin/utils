package cx.ath.mancel01.utils.actors;

import static cx.ath.mancel01.utils.M.*;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import cx.ath.mancel01.utils.actors.RemoteActors.RemoteActorContext;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import junit.framework.Assert;
import org.junit.Test;

public class PingPongRemoteTest {
    
    public static final CountDownLatch latch = new CountDownLatch(200);
    
    public static final ExecutorService service = Executors.newCachedThreadPool();
    
    public static enum Run { INSTANCE }
        
    @Test
    public void remoteActorTest() throws Exception {
        Properties host1conf = new Properties();
        Properties host2conf = new Properties();
        host1conf.load(new FileInputStream(new File("src/test/resources/pingpong/host1.properties")));
        host2conf.load(new FileInputStream(new File("src/test/resources/pingpong/host2.properties")));
        RemoteActorContext host1 = RemoteActors.newContext("host1", host1conf);
        RemoteActorContext host2 = RemoteActors.newContext("host2", host2conf);
        try {
            host1.startRemoting("127.0.0.1", 8888);
            host2.startRemoting("127.0.0.1", 8889);
            Actor pong = host2.create(PONG, "pong");
            Actor ping = host1.create(PING, "ping");
            ping.tell(Run.INSTANCE);
            latch.await();
            Assert.assertTrue(latch.getCount() <= 0);
            ping.tell(Poison.PILL);
            pong.tell(Poison.PILL);
        } finally {
            try {
                host1.stopRemoting();
                host2.stopRemoting();
            } catch (Exception e) { e.printStackTrace(); }
        }
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
                System.out.println("Received remote PONG : " + latch.getCount());
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
                System.out.println("Received remote PING : " + latch.getCount());
                ctx.from.tell("PONG", ctx.me);
            }
            return Actors.CONTINUE;
        }
    };
}
