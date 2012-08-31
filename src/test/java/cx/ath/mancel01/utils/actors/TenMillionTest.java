package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Broadcast;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.LoadBalancerActor;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TenMillionTest {

    private ActorContext system;
    private Actor router;
    private final static int nbrOfWorkers = 500000;
    final int no_of_workers = 10;
    long startedTime = System.currentTimeMillis();
    public static final CountDownLatch latch = new CountDownLatch(nbrOfWorkers);
    
    @Test
    public void testTenMillions() throws Exception {
        startedTime = System.currentTimeMillis();
        system = Actors.newContext("LoadGeneratorApp");
        final Actor appManager = system.create(new JobControllerActor(nbrOfWorkers), "jobController");
        router = system.create(LoadBalancerActor.apply(system, 10L, new Function<Unit, WorkerActor>() {
            @Override
            public WorkerActor apply(Unit t) {
                return new WorkerActor(appManager);
            }
        }), "router");
        generateLoad();
        latch.await();
        router.tell(new Broadcast(Poison.PILL));
        appManager.tell(Poison.PILL);
    }

    private void generateLoad() {
        for (int i = nbrOfWorkers; i >= 0; i--) {
            router.tell("Job Id " + i + "# send");
        }
        System.out.println("All jobs sent successfully");
    }

    public class WorkerActor implements Behavior {

        private Actor jobController;

        public WorkerActor(Actor inJobController) {
            jobController = inJobController;
        }

        @Override
        public Effect apply(Object a, Context b) {
            b.actorCtx().scheduleOnce(1000, TimeUnit.MILLISECONDS, jobController, "Done");
            return Actors.CONTINUE;
        }
    }
    
    public class JobControllerActor implements Behavior {

        int count = 0;
        final int no_of_msgs;

        public JobControllerActor(int no_of_msgs) {
            this.no_of_msgs = no_of_msgs;
        }

        @Override
        public Effect apply(Object message, Context ctx) {
            if (message instanceof String) {
                if (((String) message).compareTo("Done") == 0) {
                    count++;
                    latch.countDown();
                    if (count == no_of_msgs) {
                        long now = System.currentTimeMillis();
                        System.out.println("All messages processed in "
                                + (now - startedTime) / 1000 + " seconds");

                        System.out.println("Total Number of messages processed "
                                + count);
                        return Actors.DIE;
                    }
                }
            }
            return Actors.CONTINUE;
        }
    }
}
