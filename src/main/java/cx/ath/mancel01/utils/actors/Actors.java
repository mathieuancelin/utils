package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.F.F2;
import cx.ath.mancel01.utils.F.Function;
import java.io.Serializable;
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
            System.out.println("Dropping message [" + message + "] from [" + ctx.from.id() + "] to [" + ctx.to + "] due to severe case of death.");
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
                System.out.println("Message received in actor sink : " + message.toString());
            }
            @Override
            public void tell(Object message, Actor from) { tell(message); }

            @Override
            public String id() { return "SINK"; }

            @Override
            public boolean buzy() { return false; }
        }
    };
    
    public static enum Poison { PILL }

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

        void schedule(long every, TimeUnit unit, Runnable runnable);

        void schedule(long every, TimeUnit unit, final Actor actor, final Object message);

        void scheduleOnce(long in, TimeUnit unit, Runnable runnable);

        void scheduleOnce(long in, TimeUnit unit, final Actor actor, final Object message);
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
        public void scheduleOnce(long in, TimeUnit unit, final Actor actor, final Object message) {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    actor.tell(message);
                }
            }, in, unit);
        }
        
        @Override
        public void scheduleOnce(long in, TimeUnit unit, Runnable runnable) {
            scheduler.schedule(runnable, in, unit);
        }
        
        @Override
        public void schedule(long every, TimeUnit unit, final Actor actor, final Object message) {
            scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    actor.tell(message);
                }
            }, 0L, every, unit);
        }
        
        @Override
        public void schedule(long every, TimeUnit unit, Runnable runnable) {
            scheduler.scheduleWithFixedDelay(runnable, 0L, every, unit);
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