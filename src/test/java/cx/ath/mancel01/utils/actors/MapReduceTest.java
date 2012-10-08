/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Tuple;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

/**
 *
 * @author mathieuancelin
 */
public class MapReduceTest {
    
    @Test
    public void testAgent() {
        
        ActorContext context = Actors.newContext();
//        Actor master = context.create(null, "Master");
    }
    
    public static class CountingTask {
        
        private final String text;

        public CountingTask(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
    
        
    public static class Response {
        
        private final int nbr;

        public Response(int nbr) {
            this.nbr = nbr;
        }

        public int getNbr() {
            return nbr;
        }
    }
    
    public static class MasterActor implements Actors.Behavior {
        
        private final Function<Tuple<Actor, Actor>, CounterActor> factory;
        
        private final int numberOfCounters;

        private final List<Actor> counters = new ArrayList<Actor>();
        
        private final ActorContext context;
        
        public MasterActor(ActorContext context, Function<Tuple<Actor, Actor>, CounterActor> factory, int numberOfCounters) {
            this.factory = factory;
            this.numberOfCounters = numberOfCounters;
            this.context = context;
        }
        
        private Actor buildActorHierarchy() {
            return buildActor(numberOfCounters);
        }
        
        public Actor buildActor(int i) {
            if (i != 2 && i%2 == 0) {
                Actor son1 = buildActor(i / 2);
                Actor son2 = buildActor(i / 2);
                Actor a = context.create(factory.apply(new Tuple<Actor, Actor>(son1, son2)), "CounterActor-" + UUID.randomUUID().toString());
                counters.add(a);
                return a;
            } else {
                Actor a = context.create(factory.apply(new Tuple<Actor, Actor>(null, null)), "CounterActor-" + UUID.randomUUID().toString());
                counters.add(a);
                return a;
            }
        }

        @Override
        public Effect apply(Object message, Context ctx) {
            for (CountingTask task : M.caseClassOf(CountingTask.class, message)) {
                Actor a = buildActorHierarchy();
                a.tell(task.getText());
            }
            return Actors.CONTINUE;
        }
    }
    
    
    public static class CounterActor implements Actors.Behavior {
        
        private final Actor son1;
        
        private final Actor son2;
        
        private final CountDownLatch latch = new CountDownLatch(2);
        
        private int res = 0;

        public CounterActor(Actor son1, Actor son2) {
            this.son1 = son1;
            this.son2 = son2;
        }

        @Override
        public Effect apply(Object a, Context b) {
            for (String s  : M.caseClassOf(String.class, a)) {
                
            }
            for (Response r  : M.caseClassOf(Response.class, a)) {
                latch.countDown();
                res += r.getNbr();
            }
            return Actors.CONTINUE;
        }
    }
    
}
