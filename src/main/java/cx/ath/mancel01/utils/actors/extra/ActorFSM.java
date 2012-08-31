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

package cx.ath.mancel01.utils.actors.extra;

import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.F.Tuple;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.SimpleLogger;
import cx.ath.mancel01.utils.actors.Actors;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author mathieuancelin
 */
public abstract class ActorFSM<S, D> implements Behavior {
    
    public static enum FSMTime  {
        OUT
    }
    
    private class StateHandler<S, D> {
        public final Function<Object, Option<Move<S, D>>> handler;
        public final Long timeout;
        public final TimeUnit unit;
        public ScheduledFuture<?> future;

        public StateHandler(Function<Object, Option<Move<S, D>>> handler, Long timeout, TimeUnit unit) {
            this.handler = handler;
            this.timeout = timeout;
            this.unit = unit;
        }
        
        public StateHandler(Function<Object, Option<Move<S, D>>> handler) {
            this.handler = handler;
            this.timeout = -1L;
            this.unit = TimeUnit.SECONDS;
        }        
    }

    private S currentState;
    private Option<D> stateData;
    private Option<D> nextStateData = Option.none();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Actors.Context> context = new AtomicReference<Actors.Context>();
    private final ConcurrentHashMap<S, StateHandler<S, D>> stateHandlers =
            new ConcurrentHashMap<S, StateHandler<S, D>>();
    private Function<Object, Option<Move<S, D>>> whenUnhandled = new Function<Object, Option<Move<S, D>>>() {
        @Override
        public Option<Move<S, D>> apply(Object evt) {
            return Option.none();
        }
    };
    private Function<Tuple<S, S>, Unit> onTransition = new Function<Tuple<S, S>, Unit>() {
        @Override
        public Unit apply(Tuple<S, S> t) {
            return Unit.unit();
        }
    };
    private Function<Unit, Unit> onTermination = new Function<Unit, Unit>() {
        @Override
        public Unit apply(Unit unit) {
            return Unit.unit();
        }
    };

    public ActorFSM() {
        SimpleLogger.trace("Configuring the Finite State machine ...");
        configureFSM();
        SimpleLogger.trace("FSM configuration done");
        if (currentState == null) {
            SimpleLogger.error("FSM seems not to be initializes !!!");
        }
        running.set(true);
        SimpleLogger.trace("FSM starting with state '{}' and data '{}'", currentState, stateData);
        SimpleLogger.trace("FSM running ...");
    }

    public final void stop() {
        SimpleLogger.trace("Stopping FSM");
        running.set(false);
        SimpleLogger.trace("Running termination block ...");
        onTermination.apply(Unit.unit());
        SimpleLogger.trace("Killing actor (After all messages consumption) !!!");
        context().me.tell(Poison.PILL);
    }

    public final void onTermination(Function<Unit, Unit> handler) {
        this.onTermination = handler;
    }

    public final void onTransition(Function<Tuple<S, S>, Unit> handler) {
        this.onTransition = handler;
    }

    public abstract void configureFSM();

    public final Option<D> stateData() {
        return stateData;
    }

    public final void nextStateData(D data) {
        nextStateData = Option.apply(data);
    }

    public final void whenUnhandled(Function<Object, Option<Move<S, D>>> handler) {
        this.whenUnhandled = handler;
    }

    public final void when(S state, Function<Object, Option<Move<S, D>>> handler) {
        stateHandlers.putIfAbsent(state, new StateHandler<S, D>(handler));
    }
    
    public final void when(S state, Long timeout, TimeUnit unit, Function<Object, Option<Move<S, D>>> handler) {
        stateHandlers.putIfAbsent(state, new StateHandler<S, D>(handler, timeout, unit));
    }

    public final void startWith(S state, D data) {
        this.currentState = state;
        this.stateData = Option.apply(data);
    }

    public final Option<Move<S, D>> stay() {
        SimpleLogger.trace("Staying in state '{}' with data 'None'", currentState);
        Option<D> data = Option.none();
        return Option.some(new Move<S, D>(currentState, data));
    }

    public final Option<Move<S, D>> stay(D data) {
        SimpleLogger.trace("Staying in state '{}' with data '{}'", currentState, data);
        return Option.some(new Move<S, D>(currentState, Option.apply(data)));
    }

    public final Option<Move<S, D>> gotoState(S state) {
        SimpleLogger.trace("Goto state '{}' with data 'None'", state);
        Option<D> data = Option.none();
        return Option.some(new Move<S, D>(state, data));
    }

    public final Option<Move<S, D>> gotoState(S state, D using) {
        SimpleLogger.trace("Goto state '{}' with data '{}'", state, using);
        return Option.some(new Move<S, D>(state, Option.apply(using)));
    }

    public final Actors.Context context() {
        return context.get();
    }

    public final void reply(Object message) {
        context().from.tell(message, context().me);
    }

    @Override
    public final Effect apply(Object t, Actors.Context ctx) {
        if (running.get()) {
            context.set(ctx);
            Function<Object, Option<Move<S, D>>> handler = stateHandlers.get(currentState).handler;
            if (handler != null) {
                SimpleLogger.trace("Received event '{}' on state '{}'", t, currentState);
                Option<Move<S, D>> nextMove = handler.apply(t);
                if (nextMove.isEmpty()) {
                    SimpleLogger.trace("No state handled event, running 'unhandled' block");
                    nextMove = whenUnhandled.apply(t);
                }
                for (Move move : nextMove) {
                    S oldState = this.currentState;
                    this.currentState = (S) move.state;
                    SimpleLogger.trace("Moving from state '{}' to state '{}'", oldState, currentState);
                    SimpleLogger.trace("Running 'onTransition' block");
                    final ScheduledFuture<?> future = stateHandlers.get(oldState).future;
                    if (future != null && !future.isDone() && !future.isCancelled()) {
                        ctx.actorCtx().now(new Runnable() {
                            @Override
                            public void run() {
                                future.cancel(true);
                            }
                        });
                    }
                    Long timeout = stateHandlers.get(currentState).timeout;
                    TimeUnit unit = stateHandlers.get(currentState).unit;
                    if (timeout != -1) {
                        SimpleLogger.trace("Setting timeout for state '{}' to '{} {}'", currentState, timeout, unit);
                        stateHandlers.get(currentState).future = 
                            ctx.actorCtx().scheduleOnce(timeout, unit, ctx.me, FSMTime.OUT);
                    }
                    onTransition.apply(new Tuple<S, S>(oldState, currentState));
                    for (Object data : move.data) {
                        SimpleLogger.trace("Changing internal data to '{}'", data);
                        this.stateData = Option.apply((D) data);
                    }
                    if (this.stateData.isEmpty() && nextStateData.isDefined()) {
                        SimpleLogger.trace("Changing internal data to '{}'", nextStateData);
                        this.stateData = nextStateData;
                    }
                }
            }
            context.set(null);
        } else {
            //return Actors.DIE;
        }
        return Actors.CONTINUE;
    }

    public static class Move<S, D> {

        private final S state;
        private final Option<D> data;

        public Move(S state, D data) {
            this.state = state;
            this.data = Option.apply(data);
        }
        
        public Move(S state, Option<D> data) {
            this.state = state;
            this.data = data;
        }
    }
}