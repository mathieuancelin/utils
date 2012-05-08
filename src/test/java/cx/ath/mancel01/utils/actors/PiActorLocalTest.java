package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Broadcast;
import cx.ath.mancel01.utils.actors.Actors.LoadBalancer;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import cx.ath.mancel01.utils.actors.PiActorLocalTest.Pi.Calculate;
import cx.ath.mancel01.utils.actors.PiActorLocalTest.Pi.Master;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class PiActorLocalTest {
    
    public static final ActorContext ctx = Actors.newContext();

    @Test
    public void testPiComputation() throws Exception {
        Pi pi = new Pi();
        calculate(4, 10000, 10000);
    }

    public static class Pi {

        public static class Calculate {}

        public static class Work {

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

        public static class Result {

            private final double value;

            public Result(double value) {
                this.value = value;
            }

            public double getValue() {
                return value;
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
                for (Work work : M.caseClassOf(Work.class, t)) {
                    double result = calculatePiFor(work.getStart(), work.getNrOfElements());
                    ctx.from.tell(new Result(result));
                }
                return Actors.CONTINUE;
            }
        };
        
        public static final class Master implements Actors.Behavior {
        
            private final int nrOfMessages;
            private final int nrOfElements;
            private final CountDownLatch latch;
            private static double pi;
            private int nrOfResults;
            private List<Actor> workers = new ArrayList<Actor>();
            private LoadBalancer router;

            public Master(int nrOfWorkers, int nrOfMessages, int nrOfElements, CountDownLatch latch) {
                this.nrOfMessages = nrOfMessages;
                this.nrOfElements = nrOfElements;
                this.latch = latch;
                for (int i = 0; i < nrOfWorkers; i++) {
                    workers.add(ctx.create(Worker, "work" + i));
                }
                router = new LoadBalancer(workers);
            }
            
            @Override
            public Actors.Effect apply(Object t, Actors.Context ctx) {
                for (Calculate calc : M.caseClassOf(Calculate.class, t)) {
                    for (int start = 0; start < nrOfMessages; start++) {
                        router.tell(new Work(start, nrOfElements), ctx.me);
                    }
                    router.tell(new Broadcast(Poison.PILL));
                }
                for (Result result : M.caseClassOf(Result.class, t)) {
                    pi += result.getValue();
                    nrOfResults += 1;
                    if (nrOfResults == nrOfMessages) {
                        latch.countDown();
                        return Actors.DIE;
                    }
                }
                return Actors.CONTINUE;
            }
        };
    }

    public void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages)
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Actor master = ctx.create(new Function<Actor, Actors.Behavior>() {
            @Override
            public Behavior apply(Actor t) {
                return new Master(nrOfWorkers, nrOfMessages, nrOfElements, latch);
            }
        }, "master");
        System.out.println("Starting master");
        long start = System.currentTimeMillis();
        master.tell(new Calculate());
        latch.await();
        System.out.println(String.format(
                "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis",
                    Master.pi, (System.currentTimeMillis() - start)));
        //Thread.sleep(20000);
    }

}
