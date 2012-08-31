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

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.F.F2;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.SimpleLogger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Actors {

    public static interface Effect extends Serializable {
        Behavior getOrElse(Behavior old);
    }

    public static interface Behavior extends F2<Object, Context, Effect>, Serializable {}

    public static interface Actor {
        
        String id();
        
        boolean buzy();

        void tell(Object message);
        
        void tell(Object message, Actor from);
        
        <T> Promise<T> ask(Object message);
    }
    
    public final static Effect CONTINUE = new Effect() {

        @Override
        public Behavior getOrElse(Behavior old) {
            return old;
        }
    };

    public static class Become implements Effect {

        public final Behavior like;

        public Become(Behavior like) {
            this.like = like;
        }

        @Override
        public Behavior getOrElse(Behavior old) {
            return like;
        }
    };
    
    public final static Become DIE = new Become(new Behavior() {

        @Override
        public Effect apply(Object message, Context ctx) {
            if (message instanceof Poison) {
                return CONTINUE;
            }
            SimpleLogger.trace("Dropping message [{}, {}] from [{}] to [{}] due to severe case of death."
                    , message.getClass().getSimpleName()
                    , message
                    , ctx.from.id()
                    , ctx.to);
            return CONTINUE;
        }

        @Override
        public String toString() {
            return "DIE Behavior";
        }
    });
    
    private static final class InternalMessage {
        public final Actor from;
        public final Actor to;
        public final Object message;
        public InternalMessage(Actor from, Actor to, Object message) {
            this.from = from;
            this.message = message;
            this.to = to;
        }
    }
    
    public static final class Context {
        public final Actor from;
        public final Actor me;
        private final ActorContext ctx;
        private final String to;
        public Context(Actor me, Actor from, String to, ActorContext ctx) {
            this.from = from;
            this.me = me;
            this.ctx = ctx;
            this.to = to;
        }
        public Actor lookup(String name) {
            return ctx.lookup(name);
        }
        public ActorContext actorCtx() {
            return ctx;
        }
    }
    
    public static enum Sink implements Actor, Serializable {
        
        INSTANCE {
        
            @Override
            public void tell(Object message) {
                SimpleLogger.trace("Message received in actor sink : {}", message.toString());
            }
            @Override
            public void tell(Object message, Actor from) { tell(message); }

            @Override
            public Promise<Object> ask(Object message) {
                return Promise.pure(new Object());
            }
            @Override
            public String id() { return "SINK"; }

            @Override
            public boolean buzy() { return false; }
        }
    };
    
    public static enum Poison { PILL }
    
    public static enum Failure { FAIL }

    private static class ActorImpl extends AtomicBoolean implements Actor, Runnable {

        private final String name;
        private final Executor e;
        private final ConcurrentLinkedQueue<InternalMessage> mbox = new ConcurrentLinkedQueue<InternalMessage>();
        private Behavior behavior;
        private final ActorContext ctx;

        private ActorImpl(final Function<Actor, Behavior> initial, final String name, final Executor e, final ActorContext ctx) {
            this.name = name;
            this.e = e;
            this.ctx = ctx;
            this.behavior = new Behavior() {

                @Override
                public Effect apply(Object message, Context ctx) {
                    if (message instanceof Actor) {
                        return new Become(initial.apply((Actor) message));
                    } else {
                        return CONTINUE;
                    }
                }
            };
        }

        @Override
        public <T> Promise<T> ask(Object message) {
            final String promiseActorName =  UUID.randomUUID().toString();
            final Promise<T> promise = new Promise<T>();
            promise.onRedeem(new F.Action<Promise<T>>() {
                @Override
                public void apply(Promise<T> t) {
                    ctx.scheduleOnce(1, TimeUnit.SECONDS, new Runnable() {
                        @Override
                        public void run() {
                            ((CreationnalContextImpl) ctx).actors.remove(promiseActorName);
                        }
                    });
                }
            });
            Actor promiseActor = ctx.create(new Behavior() {
                @Override
                public Effect apply(Object a, Context b) {
                    promise.apply((T) a);
                    return Actors.DIE;
                }
            }, promiseActorName);
            tell(message, promiseActor);
            return promise;
        }

        @Override
        public final void tell(Object message) {
            tell(message, Sink.INSTANCE);
        }
        
        @Override
        public final void tell(Object message, Actor from) {
            if (behavior == DIE.like) {
                DIE.like.apply(message, new Context(this, from, this.name, ctx));
            } else {
                mbox.offer(new InternalMessage(from, this, message));
                trySchedule();
            }
        }

        @Override
        public void run() {
            try {
                set(true);
                InternalMessage context = mbox.poll();
                if (context != null && context.message != null && context.message instanceof Poison) {
                    behavior = DIE.like;
                } else {
                    behavior = behavior.apply(context.message, new Context(this, context.from, context.to.id(), ctx)).getOrElse(behavior);
                }
            } finally {
                set(false);
                trySchedule();
            }
        }

        @Override
        public final String toString() {
            return "localactor://" + name;
        }

        private void trySchedule() {
            if (!mbox.isEmpty() && compareAndSet(false, true)) {
                try {
                    e.execute(this);
                } catch (RejectedExecutionException ree) {
                    set(false);
                    throw ree;
                }
            }
        }

        @Override
        public String id() {
            return name;
        }

        @Override
        public boolean buzy() {
            return get();
        }
    }
    
    public static class LoadBalancerActor implements Behavior {
        
        private final LoadBalancer balancer;
        
        public static LoadBalancerActor apply(ActorContext system, long number, Function<Unit, ? extends Behavior> of) {
            if (number > 0) {
                List<Actor> workers = new ArrayList<Actor>();
                for (long i = 0; i < number; i++) {
                    Behavior b = of.apply(Unit.unit());
                    workers.add(system.create(b, b.getClass().getSimpleName() + "__" + i));
                } 
                return new Actors.LoadBalancerActor(workers);
            } else {
                throw new RuntimeException("You can't submit 0");
            }
        }
        
        public LoadBalancerActor(List<Actor> actors) {
            this.balancer = new LoadBalancer(actors);
        }

        @Override
        public Effect apply(Object evt, Context ctx) {
            for (Broadcast b : M.caseClassOf(Broadcast.class, evt)) {
                balancer.tell(b);
                return Actors.CONTINUE;
            }
            balancer.tell(evt, ctx.from);
            return Actors.CONTINUE;
        }
    }
    
    public static class LoadBalancer implements Serializable {

        private final List<Actor> actors;
        private Iterator<Actor> it;

        public LoadBalancer(List<Actor> actors) {
            this.actors = actors;
            this.it = actors.iterator();
        }

        public final void tell(Broadcast msg) {
            broadcast(msg.message, msg.from);
        }
        
        public final void tell(Object msg) {
            chooseAndSend(msg, null);
        }

        public final void tell(Object msg, Actor from) {
            chooseAndSend(msg, from);
        }

        private void chooseAndSend(Object msg, Actor from) {
            if (!it.hasNext()) {
                it = actors.iterator();
            }
            Actor a = it.next();
            if (!a.buzy()) {
                a.tell(msg, from);
            } else {
                boolean sent = false;
                for (Actor bis : actors) {
                    if (!bis.buzy()) {
                        a.tell(msg, from);
                        sent = true;
                    }
                }
                if (!sent) {
                    a.tell(msg, from);
                }
            }
        }

        private void broadcast(Object message, Actor from) {
            for (Actor actor : actors) {
                actor.tell(message, from);
            }
        }
    }
    
    public static class BroadcasterActor implements Behavior {
        
        private final Broadcaster broadcaster;
        
        public BroadcasterActor(List<Actor> actors) {
            this.broadcaster = new Broadcaster(actors);
        }

        @Override
        public Effect apply(Object evt, Context ctx) {
            broadcaster.tell(evt, ctx.from);
            return Actors.CONTINUE;
        }
    }
        
    public static class Broadcaster implements Serializable {

        private final List<Actor> actors;

        public Broadcaster(List<Actor> actors) {
            this.actors = actors;
        }

        public final void tell(Object msg) {
            for (Actor actor : actors) {
                actor.tell(msg);
            }
        }

        public final void tell(Object msg, Actor from) {
            for (Actor actor : actors) {
                actor.tell(msg, from);
            }
        }
    }

    public static class Broadcast implements Serializable {

        public final Object message;
        public final Actor from;

        public Broadcast(Object message, Actor from) {
            this.message = message;
            this.from = from;
        }

        public Broadcast(Object message) {
            this.message = message;
            this.from = Actors.Sink.INSTANCE;
        }
    }

    public static interface ActorContext {
        
        public Actor lookup(String name);

        public Actor create(final Function<Actor, Behavior> initial, final String name);

        public Actor create(final Function<Actor, Behavior> initial, final String name, final Executor e);

        public Actor create(final Behavior initial, final String name);

        public Actor create(final Behavior initial, final String name, final Executor e);

        public void clear();
        
        void now(Runnable runnable);
        
        <T> Promise<T> now(F.Callable<T> callable);

        ScheduledFuture<?> schedule(long every, TimeUnit unit, Runnable runnable);

        ScheduledFuture<?> schedule(long every, TimeUnit unit, final Actor actor, final Object message);

        ScheduledFuture<?> scheduleOnce(long in, TimeUnit unit, Runnable runnable);
        
        <T> Promise<T>  scheduleOnce(long in, TimeUnit unit, F.Callable<T> callable);

        ScheduledFuture<?> scheduleOnce(long in, TimeUnit unit, final Actor actor, final Object message);
    }
    
    static class CreationnalContextImpl implements ActorContext {
        
        private final ConcurrentHashMap<String, Actor> actors = new ConcurrentHashMap<String, Actor>();
    
        private final ExecutorService defaultExec = Executors.newCachedThreadPool();
        
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        private ExecutorService service;

        protected final String id;

        public CreationnalContextImpl(String id) {
            this.id = id;
            this.service = defaultExec;
        }
        
        public CreationnalContextImpl(String id, ExecutorService service) {
            this.id = id;
            this.service = service;
        }

        ConcurrentHashMap<String, Actor> getActors() {
            return actors;
        }
        
        @Override
        public Actor lookup(String name) {
            if (actors.containsKey(name)) {
                return actors.get(name);
            }
            throw new RuntimeException("Actor \"" + name + "\" don't exist in context \"" + id + "\". Please create it.");
        }

        @Override
        public Actor create(final Function<Actor, Behavior> initial, final String name) {
            return create(initial, name, service);
        }

        @Override
        public Actor create(final Function<Actor, Behavior> initial, final String name, final Executor e) {
            if (!actors.containsKey(name)) {
                final Actor a = new ActorImpl(initial, name, e, this);
                a.tell(a);
                actors.putIfAbsent(name, a);
                return a;
            } else {
                return actors.get(name);
            }
        }

        @Override
        public Actor create(final Behavior initial, final String name) {
            return create(initial, name, service);
        }

        @Override
        public Actor create(final Behavior initial, final String name, final Executor e) {
            return create(new Function<Actor, Behavior>() {
                @Override
                public Behavior apply(Actor t) {
                    return initial;
                }
            }, name, e);
        }

        @Override
        public void clear() {
            actors.clear();
        }

        @Override
        public String toString() {
            return "Actor context : " + id;
        }
        
        @Override
        public ScheduledFuture<?> scheduleOnce(long in, TimeUnit unit, final Actor actor, final Object message) {
            return scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    actor.tell(message);
                }
            }, in, unit);
        }
        
        @Override
        public ScheduledFuture<?> scheduleOnce(long in, TimeUnit unit, Runnable runnable) {
            return scheduler.schedule(runnable, in, unit);
        }
        
        @Override
        public ScheduledFuture<?> schedule(long every, TimeUnit unit, final Actor actor, final Object message) {
            return scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    actor.tell(message);
                }
            }, 0L, every, unit);
        }
        
        @Override
        public ScheduledFuture<?> schedule(long every, TimeUnit unit, Runnable runnable) {
            return scheduler.scheduleWithFixedDelay(runnable, 0L, every, unit);
        }

        @Override
        public void now(Runnable runnable) {
            scheduleOnce(0, TimeUnit.MILLISECONDS, runnable);
        }

        @Override
        public <T> Promise<T> now(final F.Callable<T> callable) {
            final Promise<T> promise = new Promise<T>();
            now(new Runnable() {
                @Override
                public void run() {                    
                    promise.apply(callable.apply());
                }
            });
            return promise;
        }

        @Override
        public <T> Promise<T> scheduleOnce(long in, TimeUnit unit, final F.Callable<T> callable) {
            final Promise<T> promise = new Promise<T>();
            scheduleOnce(in, unit, new Runnable() {
                @Override
                public void run() {
                    promise.apply(callable.apply());
                }
            });
            return promise;
        }
    }
    
    private final static ConcurrentHashMap<String, ActorContext> CTXS = new ConcurrentHashMap<String, ActorContext>();

    public static ActorContext newContext() {
        return newContext(UUID.randomUUID().toString());        
    } 
    
    public static ActorContext newContext(ExecutorService service) {
        return newContext(UUID.randomUUID().toString(), service);        
    } 
    
    public static ActorContext newContext(String id) {
        if (!CTXS.containsKey(id)) {
            CreationnalContextImpl c = new CreationnalContextImpl(id);
            CTXS.putIfAbsent(id, c);
        }
        return CTXS.get(id);       
    } 
    
    public static ActorContext newContext(String id, ExecutorService service) {
        if (!CTXS.containsKey(id)) {
            CreationnalContextImpl c = new CreationnalContextImpl(id, service);
            CTXS.putIfAbsent(id, c);
        }
        return CTXS.get(id);
    } 
}