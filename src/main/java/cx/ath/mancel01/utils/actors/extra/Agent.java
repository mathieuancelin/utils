package cx.ath.mancel01.utils.actors.extra;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Unit;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Agent<T> implements Function<Unit, T> {
    
    public static abstract class Mutator<T> {
        private final Function<T, T> f;
        public Mutator(Function<T, T> f) { this.f = f; }
        public Function<T, T> f() { return f; }
    }
    public static class Update<T> extends Mutator<T> {
        public Update(Function<T, T> f) { super(f); }
    }
    public static class Alter<T> extends Mutator<T> {
        public Alter(Function<T, T> f) { super(f); }
    }
    public static enum Get {
        GET
    }
    
    private final AtomicReference<T> ref;
    private final Actor updater;
    private final String name;
    private final Actors.ActorContext ctx;
    
    public Agent(T initialValue, Actors.ActorContext ctx) {
        this.name = UUID.randomUUID().toString();
        this.ctx = ctx;
        this.ref = new AtomicReference<T>(initialValue);
        this.updater = ctx.create(new AgentUpdater<T>(ref), name);
    }
    
    @Override
    public T apply(Unit t) {
        return get();
    }
    
    public void close() {
        updater.tell(Actors.Poison.PILL);
    }
    
    public void send(final T newValue) {
        send(new Function<T, T>() {
            @Override
            public T apply(T old) {
                return newValue;
            }
        });
    }
    
    public void send(Function<T, T> f) {
        updater.tell(new Update<T>(f));
    }
    
    public Promise<T> alter(final T newValue) {
        return updater.ask(new Update<T>(new Function<T, T>() {
            @Override
            public T apply(T old) {
                return newValue;
            }
        }));
    }
    
    public void update(T newValue) {
        send(newValue);
    }
    
    public T get() {
        return ref.get();
    }
    
    public Promise<T> future() {
        return updater.ask(Get.GET);
    }
    
    public <B> Agent<B> map(Function<T, B> f) {
        return new Agent<B>(f.apply(get()), ctx);
    }
    
    public <B> Agent<B> flatMap(Function<T, Agent<B>> f) {
        return f.apply(get());
    }
    
    public <U> void foreach(Function<T, U> f) {
        f.apply(get());
    }
    
    public T await(Long timeout, TimeUnit unit) {
        try {
            return future().get(timeout, unit);
        } catch (Exception ex) {
            throw new F.ExceptionWrapper(ex);
        }
    }
    
    private static class AgentUpdater<T> implements Actors.Behavior {
        
        private final AtomicReference<T> ref;

        public AgentUpdater(AtomicReference<T> ref) {
            this.ref = ref;
        }

        @Override
        public Effect apply(Object evt, Context ctx) {
            for (Get get : M.caseClassOf(Get.class, evt)) {
                ctx.from.tell(ref.get());
            }
            for (Update<T> update : M.caseClassOf(Update.class, evt)) {
                ref.set(update.f().apply(ref.get()));
            }
            for (Alter<T> alter : M.caseClassOf(Alter.class, evt)) {
                T newValue = alter.f().apply(ref.get());
                ref.set(newValue);
                ctx.from.tell(newValue);
            }
            return Actors.CONTINUE;
        }
    }
}
