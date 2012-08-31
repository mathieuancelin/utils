/*
 *  Copyright 2011-2012 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Tuple;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.SimpleLogger;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.extra.ActorFSM;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

public class FSMTest {

    public static Object obj = null;

    @Test
    public void fsmTest() throws Exception {
        SimpleLogger.enableColors(true);
        SimpleLogger.enableTrace(true);
        ActorContext ctx = Actors.newContext();
        Actor testActor = ctx.create(TEST, "TESTACTOR");
        Actor buncher = ctx.create(new Buncher(), "buncher");
        buncher.tell(new SetTarget(testActor));
        buncher.tell(new Queue(42));
        buncher.tell(new Queue(43));
        //buncher.tell(new Flush());
        //expectMsg(Batch(Seq(42, 43)))
        waitAndSout();
        buncher.tell(new Queue(44));
        buncher.tell(new Flush());
        buncher.tell(new Queue(45));
        waitAndSout();
        waitAndSout();
        //expectMsg(Batch(Seq(44)))
        //expectMsg(Batch(Seq(45)))
    }
    
    public static CountDownLatch latch = new CountDownLatch(1);

    
    public static void waitAndSout() {
        try {
            latch.await();
            latch = new CountDownLatch(1);
            System.out.println(obj);
            obj = null;
        } catch (InterruptedException ex) {
            Logger.getLogger(FSMTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static final Actors.Behavior TEST = new Actors.Behavior() {

        @Override
        public Actors.Effect apply(Object t, Actors.Context ctx) {
            obj = t;
            latch.countDown();
            return Actors.CONTINUE;
        }
    };

    public static class SetTarget {

        public final Actor ref;

        public SetTarget(Actor ref) {
            this.ref = ref;
        }

        @Override
        public String toString() {
            return "SetTarget{" + ref + '}';
        }
    }

    public static class Queue {

        public final Object obj;

        public Queue(Object obj) {
            this.obj = obj;
        }

        @Override
        public String toString() {
            return "Queue{" + obj + '}';
        }
    }

    public static class Flush {

        @Override
        public String toString() {
            return "Flush";
        }
    }

    public static class Batch {

        public final List<Object> obj;

        public Batch(List<Object> obj) {
            this.obj = obj;
        }

        @Override
        public String toString() {
            return "Batch{" + obj + '}';
        }
    }

    public static enum State {

        Idle, Active
    }

    public static interface Data {
    }

    public static class Uninitialized implements Data {

        @Override
        public String toString() {
            return "Uninitialized";
        }
    }

    public static class Todo implements Data {

        public final Actor target;
        public final List<Object> queue;

        public Todo(Actor target, List<Object> queue) {
            this.target = target;
            this.queue = queue;
        }

        @Override
        public String toString() {
            return "Todo{" + "target=" + target + ", queue=" + queue + '}';
        }
    }

    public static class Buncher extends ActorFSM<State, Data> {

        @Override
        public void configureFSM() {
            
            startWith(State.Idle, new Uninitialized());
            
            when(State.Idle, new Function<Object, Option<Move<State, Data>>>() {
                @Override
                public Option<Move<State, Data>> apply(Object evt) {
                    for (SetTarget st : M.caseClassOf(SetTarget.class, evt)) {
                        return stay(new Todo(st.ref, new ArrayList<Object>()));
                    }
                    return Option.none();
                }
            });
            when(State.Active, 1L, TimeUnit.SECONDS, new Function<Object, Option<Move<State, Data>>>() {
                @Override
                public Option<Move<State, Data>> apply(Object evt) {
                    for (Flush fl : M.caseClassOf(Flush.class, evt)) {
                        return gotoState(State.Idle, new Todo(((Todo) stateData().get()).target, new ArrayList<Object>()));
                    }
                    for (FSMTime fsm : M.caseClassOf(FSMTime.class, evt)) {
                        return gotoState(State.Idle, new Todo(((Todo) stateData().get()).target, new ArrayList<Object>()));
                    }
                    return Option.none();
                }
            });
            whenUnhandled(new Function<Object, Option<Move<State, Data>>>() {
                @Override
                public Option<Move<State, Data>> apply(Object evt) {
                    for (Queue queue : M.caseClassOf(Queue.class, evt)) {
                        List<Object> objs = ((Todo) stateData().get()).queue;
                        objs.add(queue.obj);
                        return gotoState(State.Active, new Todo(((Todo) stateData().get()).target, objs));
                    }
                    return stay();
                }
            });
            onTransition(new Function<Tuple<State, State>, Unit>() {
                @Override
                public Unit apply(Tuple<State, State> states) {
                    if (states._1.equals(State.Active) && states._2.equals(State.Idle)) {
                        for (Data data : stateData()) {
                            for (Todo todo : M.caseClassOf(Todo.class, data)) {
                                todo.target.tell(new Batch(todo.queue), context().me);
                            }
                        }
                    }
                    return Unit.unit();
                }
            });
        }
    }
}
