package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.C.Function;
import cx.ath.mancel01.utils.F.Option;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Actor extends Thread {
    
    private static final BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<Runnable>(500);
    
    private static ExecutorService executor = Executors.newCachedThreadPool();
//            new ThreadPoolExecutor(0, Integer.MAX_VALUE,
//                      60L, TimeUnit.SECONDS, tasks);
    
    static {
        Runtime.getRuntime().addShutdownHook(
            new Thread() {
                @Override
                public void run() {
                    executor.shutdownNow();
                }
            }
        );
    }
    
    private static class Message {
        Object payload;
        Option<Actor> sender;
        public Message(Object payload, Option<Actor> sender) {
            this.payload = payload;
            this.sender = sender;
        }
    }
    
    private ConcurrentLinkedQueue<Message> mailbox = new ConcurrentLinkedQueue<Message>();
        
    private Function<Object> react;
    
    private AtomicBoolean started = new AtomicBoolean(false);
    
    protected Option<Actor> sender = Option.none();
    
    protected void before() {};
    
    protected void after() {};

    @Override
    public void run() {
        mailbox.clear();
        started.compareAndSet(false, true);
        before();
        act();
        after();
        started.compareAndSet(true, false);
    }
    
    public abstract void act();
    
    public final Actor me() {
        return this;
    }
    
    public final void loop(Function react) {
        this.react = react;
        while(started.get()) {
            Message ret = mailbox.poll();
            if (ret != null) {
                if (ret.payload.getClass().equals(PoisonPill.class)) {
                    stopActor();
                } else {
                    sender = ret.sender;
                    react.apply(ret.payload);
                    sender = Option.none();
                }
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    
    public final void react(Function<Object> react) {
        this.react = react;
        Message ret = mailbox.poll();
        if (ret != null) {
            if (ret.payload.getClass().equals(PoisonPill.class)) {
                stopActor();
            } else {
                sender = ret.sender;
                react.apply(ret.payload);
                sender = Option.none();
            }
        }
    }
    
    public final void send(Object msg) {
        Option<Actor> opt = Option.none();
        mailbox.add(new Message(msg, opt));
    }
    
    public final void send(Object msg, Actor from) {
        Option<Actor> opt = Option.maybe(from);
        mailbox.add(new Message(msg, opt));
    }
    
    public final Actor stopActor() {
        started.compareAndSet(true, false);
        return this;
    }
    
    public final Actor startActor() {
        started.compareAndSet(false, true);
        executor.submit(this);
        return this;
    }
    
    public static class LoadBalancer {
        
        private final List<Actor> actors;
        // TODO : need a better heuristic for real load balancing
        private Iterator<Actor> it;

        public LoadBalancer(List<Actor> actors) {
            this.actors = actors;
            this.it = actors.iterator();
        }
        
        public final void send(Object msg) {
            if (msg.getClass().equals(Broadcast.class)) {
                Broadcast b = (Broadcast) msg;
                broadcast(b.message, b.from.getOrElse(null));
            } else {
                if (!it.hasNext()) {
                    it = actors.iterator();
                }
                it.next().send(msg);
            }
        }

        public final void send(Object msg, Actor from) {
            if (msg.getClass().equals(Broadcast.class)) {
                Broadcast b = (Broadcast) msg;
                broadcast(b.message, b.from.getOrElse(from));
            } else {
                if (!it.hasNext()) {
                    it = actors.iterator();
                }
                it.next().send(msg, from);
            }
        }
        
        private void broadcast(Object message, Actor from) {
            for (Actor actor : actors) {
                if (from == null) {
                   actor.send(message);
                } else {
                   actor.send(message, from);
                }
            }
        }
    }
    
    public static abstract class NamedActor extends Actor {
        
        protected final String name;

        public NamedActor(String name) {
            super();
            this.name = name;
        }

        public String name() {
            return name;
        }
    }
    
    public static class Broadcast {
        
        private final Object message;
        
        private final Option<Actor> from;

        public Broadcast(Object message, Option<Actor> from) {
            this.message = message;
            this.from = from;
        }
        
        public Broadcast(Object message) {
            this.message = message;
            this.from = Option.none();
        }

        public Option<Actor> getFrom() {
            return from;
        }

        public Object getMessage() {
            return message;
        }
    }
    
    public static class PoisonPill {}
}
