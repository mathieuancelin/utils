package cx.ath.mancel01.utils;

import java.util.Iterator;
import cx.ath.mancel01.utils.C.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import static cx.ath.mancel01.utils.M.*;

public class PiActorTest {

    @Test
    public void testPiComputation() throws Exception {
        Pi pi = new Pi();
        pi.calculate(4, 10000, 10000);
    }

    public static class Pi {

        public static class Calculate {
        }

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

        public static class Worker extends Actor {

            private double calculatePiFor(int start, int nrOfElements) {
                double acc = 0.0;
                for (int i = start * nrOfElements; i <= ((start + 1) * nrOfElements - 1); i++) {
                    acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1);
                }
                return acc;
            }

            @Override
            public void act() {
                loop(new Function<Object>() {
                    @Override
                    public void apply(Object msg) {
                        for (Work work : with(caseClassOf(Work.class)).match(msg)) {
                            double result = calculatePiFor(work.getStart(), work.getNrOfElements());
                            sender.get().send(new Result(result));
                        }
                        for (PoisonPill p : with(caseClassOf(PoisonPill.class)).match(msg)) {
                            me().stopActor();
                        }
                    }
                });
            }
        }

        public static class Master extends Actor {

            private final int nrOfMessages;
            private final int nrOfElements;
            private final CountDownLatch latch;
            private double pi;
            private int nrOfResults;
            private long start;
            private List<Actor> workers = new ArrayList<Actor>();

            public Master(int nrOfWorkers, int nrOfMessages, int nrOfElements, CountDownLatch latch) {
                this.nrOfMessages = nrOfMessages;
                this.nrOfElements = nrOfElements;
                this.latch = latch;
                for (int i = 0; i < nrOfWorkers; i++) {
                    workers.add(new Worker().startActor());
                }
            }
            
            @Override
            public void act() {
                System.out.println("Starting master");
                start = System.currentTimeMillis();
                loop(new Function() {
                    @Override
                    public void apply(Object t) {
                        for (Calculate c : with(caseClassOf(Calculate.class)).match(t)) {
                            Iterator<Actor> it = workers.iterator();
                            for (int start = 0; start < nrOfMessages; start++) {
                                if (!it.hasNext()) {
                                    it = workers.iterator();
                                }
                                it.next().send(new Work(start, nrOfElements), me());
                            }
                            for (Actor actor : workers) {
                                actor.send(new PoisonPill(), me());
                            }                            
                        }
                        for (Result result : with(caseClassOf(Result.class)).match(t)) {
                            pi += result.getValue();
                            nrOfResults += 1;
                            if (nrOfResults == nrOfMessages) {
                                me().stopActor();
                                System.out.println(String.format(
                                    "\n\tPi estimate: \t\t%s\n\tCalculation time: \t%s millis",
                                        pi, (System.currentTimeMillis() - start)));
                                latch.countDown();
                            }
                        }
                    }
                });
            }
        }

        public void calculate(final int nrOfWorkers, final int nrOfElements, final int nrOfMessages)
                throws Exception {
            final CountDownLatch latch = new CountDownLatch(1);
            Actor master = new Master(nrOfWorkers, nrOfMessages, nrOfElements, latch).startActor();
            master.send(new Calculate());
            latch.await();
        }
    }
}
