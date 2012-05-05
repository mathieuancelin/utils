package cx.ath.mancel01.utils.actors;

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F.Option;
import cx.ath.mancel01.utils.M;
import cx.ath.mancel01.utils.actors.Actors.Actor;
import cx.ath.mancel01.utils.actors.Actors.ActorContext;
import cx.ath.mancel01.utils.actors.Actors.Behavior;
import cx.ath.mancel01.utils.actors.Actors.Context;
import cx.ath.mancel01.utils.actors.Actors.Effect;
import cx.ath.mancel01.utils.actors.Actors.Poison;
import java.util.UUID;

public class AsyncIteratees {
    
    public static final class Elem<I> {
        private final I e;
        public Elem(I e) { this.e = e; }
        public Option<I> get() { return Option.apply(e); }
    }
    public static enum EOF { INSTANCE }
    public static enum Empty { INSTANCE }
    private static enum Run { INSTANCE }
    public static enum Done { INSTANCE }
    public static enum Cont { INSTANCE }
    public static final class Error<E> {
        public final E error;
        public Error(E error) {
            this.error = error;
        }
    }
    
    public static abstract class Iteratee<I, O> implements Behavior {
        public abstract Promise<O> getAsyncResult();
    }
    
    public static abstract class Enumerator<I> implements Behavior {
        @Override
        public Effect apply(Object msg, Context ctx) {
            for (Run run : M.caseClassOf(Run.class, msg)) {
                sendNext(msg, ctx);
            }
            for (Cont cont : M.caseClassOf(Cont.class, msg)) {
                sendNext(msg, ctx);
            }
            for (Done done : M.caseClassOf(Done.class, msg)) {
                ctx.from.tell(Poison.PILL, ctx.me);
                return Actors.DIE;
            }
            for (Error err : M.caseClassOf(Error.class, msg)) {
                ctx.from.tell(Poison.PILL, ctx.me);
                System.err.println(err.error);
                return Actors.DIE;            
            }
            return Actors.CONTINUE;
        }
        private void sendNext(Object msg, Context ctx) {
            if (!hasNext()) {
                ctx.from.tell(EOF.INSTANCE, ctx.me);
            } else {
                Option<I> optElemnt = next();
                for (I element : optElemnt) {
                    ctx.from.tell(new Elem<I>(element), ctx.me);
                }
                if (optElemnt.isEmpty()) {
                    ctx.from.tell(Empty.INSTANCE, ctx.me);
                }
            }
        }
        public abstract boolean hasNext();
        public abstract Option<I> next();
        public <O> Promise<O> applyOn(Iteratee<I, O> it) {
            Promise<O> res = it.getAsyncResult();
            ActorContext context = Actors.newContext();
            Actor enumerator = context.create(this, UUID.randomUUID().toString());
            Actor iteratee = context.create(it, UUID.randomUUID().toString());
            enumerator.tell(Run.INSTANCE, iteratee);
            return res;
        }
    }    
}
