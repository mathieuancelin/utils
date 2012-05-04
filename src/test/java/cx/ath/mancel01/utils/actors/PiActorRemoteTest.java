package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.M;
import static cx.ath.mancel01.utils.M.*;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Broadcast;
import cx.ath.mancel01.utils.actors.Actors.LoadBalancer;
import cx.ath.mancel01.utils.actors.RemoteActors.RemoteActorContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class PiActorRemoteTest {
    
    final static CountDownLatch latch = new CountDownLatch(5);
    
    @Test
    public void testPiComputation() throws Exception {
        calculate(4, 10000, 10000);
    }
    
    public static enum End { INSTANCE }

    public static class Calculate implements Serializable {}

    public static class Work implements Serializable {

        private final int start;
        private final int nrOfElements;

        public Work(int start, int nrOfElements) {
            this.start = start;
            this.nrOfElements = nrOfElements;
        }

        public int getStart() {
            return start;
        }

        public int getNrOfElements() {
            return nrOfElements;
        }
    }

    public static class Result implements Serializable {

        private final double value;

        public Result(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Result : " + value;
        }
    }

    public static final Actors.Behavior Worker = new Actors.Behavior() {

        private double calculatePiFor(int start, int nrOfElements) {
            double acc = 0.0;
            for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
            }
            return acc;
        }

        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            for (Work work : caseClassOf(Work.class, t)) {
                double result = calculatePiFor(work.getStart(), work.getNrOfElements());
                ctx.lookup("master").tell(new Result(result), ctx.me);
            }
            for (End end : caseClassOf(End.class, t)) {
                ctx.lookup("master").tell(End.INSTANCE, ctx.me);
                latch.countDown();
                return Actors.DIE;
            }
            return Actors.CONTINUE;
        }
    };

    public static final class Master implements Actors.Behavior {

        private final int nrOfMessages;
        private final int nrOfElements;
        private final CountDownLatch latch;
        private static double pi;
        private CountDownLatch ends = new CountDownLatch(4);
        
        public Master(int nrOfMessages, int nrOfElements, CountDownLatch latch) {
            this.nrOfMessages = nrOfMessages;
            this.nrOfElements = nrOfElements;
            this.latch = latch;
        }

        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            for (Calculate calc : caseClassOf(Calculate.class, t)) {
                for (int start = 0; start < nrOfMessages; start++) {
                    ctx.lookup("balancer").tell(new Work(start, nrOfElements), ctx.me);
                }
                ctx.lookup("balancer").tell(new Broadcast(End.INSTANCE), ctx.me);
            }
            for (Result result : caseClassOf(Result.class, t)) {
                pi += result.getValue();
            }
            for (End end : caseClassOf(End.class, t)) {
                ends.countDown();
                if (ends.getCount() == 0) {
                    latch.countDown();
                    return Actors.DIE;
                }
            }
            return Actors.CONTINUE;
        }
    };

    public static final class Balancer implements Actors.Behavior {

        private List<Actor> workers = new ArrayList<Actor>();
        private LoadBalancer router;

        public Balancer(int nrOfWorkers, RemoteActorContext rctx) {
            for (int i = 0; i < nrOfWorkers; i++) {
                workers.add(rctx.create(Worker, "work" + i));
            }
            router = new LoadBalancer(workers);
        }

        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            for (Work work : M.caseClassOf(Work.class, t)) {
                router.tell(work, ctx.from);
            }
            for (Broadcast broad : M.caseClassOf(Broadcast.class, t)) {
                router.tell(broad);
                if (broad.message instanceof End) {
                    return Actors.DIE;
                }
            }
            return Actors.CONTINUE;
        }
    };
   
    private void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages)
            throws Exception {
        Properties host1conf = new Properties();
        Properties host2conf = new Properties();
        host1conf.load(new FileInputStream(new File("src/test/resources/pi/host1.properties")));
        host2conf.load(new FileInputStream(new File("src/test/resources/pi/host2.properties")));
        final RemoteActors.RemoteActorContext host1 = RemoteActors.newContext("host11", host1conf);
        final RemoteActors.RemoteActorContext host2 = RemoteActors.newContext("host21", host2conf);
        try {
            host1.startRemoting("127.0.0.1", 8888);
            host2.startRemoting("127.0.0.1", 8889);
            Actor master = host1.create(new Function<Actor, Actors.Behavior>() {
                @Override
                public Behavior apply(Actor t) {
                    return new Master(nrOfElements, nrOfMessages, latch);
                }
            }, "master");

            Actor balancer = host2.create(new Function<Actor, Actors.Behavior>() {
                @Override
                public Behavior apply(Actor t) {
                    return new Balancer(nrOfWorkers, host2);
                }
            }, "balancer");
            System.out.println("Starting master");
            long start = System.currentTimeMillis();
            master.tell(new Calculate());
            latch.await();
            System.out.println(String.format(
                    "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis",
                        Master.pi, (System.currentTimeMillis() - start)));
        } finally {
            try {
                host1.stopRemoting();
                host2.stopRemoting();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
