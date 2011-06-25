package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.C.Function;
import cx.ath.mancel01.utils.F.Option;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small Actors library for multi-threaded programming models.
 * 
 * @author Mathieu ANCELIN
 */
public class Actors {
    
    private static final ConcurrentHashMap<String, ActorRef> actors = 
            new ConcurrentHashMap<String, ActorRef>();
    
    private static ActorRef getActor(String name) {
        // here manage remote and local actors
        return actors.get(name);
    }
    
    private static void register(String name, ActorRef actor) {
        // here manage remote and local actors
        actors.putIfAbsent(name, actor);
    }
    
    private static void unregister(String name) {
        // here manage remote and local actors
        actors.remove(name);
    }
    
    public static Option<ActorRef> forName(String name) {
        return Option.maybe(Actors.getActor(name));
    }
    
    public static void shutdownAll() {
        Actor.executor.shutdownNow();
    }
    
    public static interface ActorRef {

        void send(Object msg);

        void send(Object msg, String from);
    }

    public static interface LocalActorRef extends ActorRef {

        void send(Object msg, ActorRef from);
    }

    public static abstract class Actor extends Thread implements LocalActorRef {

        private static final BlockingQueue<Runnable> tasks = new ArrayBlockingQueue<Runnable>(500);
        
        private static ExecutorService executor = Executors.newCachedThreadPool();

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
            
            Option<ActorRef> sender;

            public Message(Object payload, Option<ActorRef> sender) {
                this.payload = payload;
                this.sender = sender;
            }
        }
        private ConcurrentLinkedQueue<Message> mailbox = 
                new ConcurrentLinkedQueue<Message>();
        
        private Function<Object> react;
        
        private AtomicBoolean started = new AtomicBoolean(false);
        
        private AtomicBoolean buzy = new AtomicBoolean(false);
        
        protected Option<ActorRef> sender = Option.none();
        
        protected final String uuid;

        public Actor() {
            uuid = UUID.randomUUID().toString();
            Actors.register(uuid, this);
        }
        
        public void unregister() {
            Actors.unregister(uuid);
        }

        protected void before() {}
    
        protected void after() {}

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

        /**
         * Method to loop the reception function to read all messages
         * in the mailbox one by one. 
         * Once launch, the loop will end with the stopActor method
         * or with a poison pill.
         */
        public final void loop(Function react) {
            this.react = react;
            while (started.get()) {
                Message ret = mailbox.poll();
                if (ret != null) {
                    if (ret.payload.getClass().equals(PoisonPill.class)) {
                        stopActor();
                    } else {
                        sender = ret.sender;
                        buzy.compareAndSet(false, true);
                        react.apply(ret.payload);
                        buzy.compareAndSet(true, false);
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

        /**
         * Read only one message in the mailbox with the function.
         */
        public final void react(Function<Object> react) {
            this.react = react;
            Message ret = mailbox.poll();
            if (ret != null) {
                if (ret.payload.getClass().equals(PoisonPill.class)) {
                    stopActor();
                } else {
                    sender = ret.sender;
                    buzy.compareAndSet(false, true);
                    react.apply(ret.payload);
                    buzy.compareAndSet(true, false);
                    sender = Option.none();
                }
            }
        }

        @Override
        public final void send(Object msg) {
            Option<ActorRef> opt = Option.none();
            mailbox.add(new Message(msg, opt));
        }

        @Override
        public final void send(Object msg, ActorRef from) {
            Option<ActorRef> opt = Option.maybe(from);
            mailbox.add(new Message(msg, opt));
        }
        
        @Override
        public final void send(Object msg, String from) {
            send(msg, Actors.getActor(from));
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
    }

    public static class LoadBalancer {

        private final List<Actor> actors;
        
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
                chooseAndSend(msg, null);
            }
        }

        public final void send(Object msg, Actor from) {
            if (msg.getClass().equals(Broadcast.class)) {
                Broadcast b = (Broadcast) msg;
                broadcast(b.message, b.from.getOrElse(from));
            } else {
                chooseAndSend(msg, from);
            }
        }

        private void chooseAndSend(Object msg, Actor from) {
            if (!it.hasNext()) {
                it = actors.iterator();
            }
            Actor a = it.next();
            if (!a.buzy.get()) {
                a.send(msg, from);
            } else {
                boolean sent = false;
                for (Actor bis : actors) {
                    if (!bis.buzy.get()) {
                        a.send(msg, from);
                        sent = true;
                    }
                }
                if (!sent) {
                    a.send(msg, from);
                }
            }
        }

        private void broadcast(Object message, Actor from) {
            for (Actor actor : actors) {
                actor.send(message, from);
            }
        }
    }
    
    public static class Broadcaster {

        private final List<Actor> actors;
        
        public Broadcaster(List<Actor> actors) {
            this.actors = actors;
        }

        public final void send(Object msg) {
            for (Actor actor : actors) {
                actor.send(msg);
            }
        }

        public final void send(Object msg, Actor from) {
            for (Actor actor : actors) {
                actor.send(msg, from);
            }
        }
    }

    public static abstract class NamedActor extends Actor {

        protected final String name;

        public NamedActor(String name) {
            super();
            this.name = name;
            Actors.unregister(uuid);
            Actors.register(name, this);
        }

        public String name() {
            return name;
        }

        @Override
        public void unregister() {
            Actors.unregister(name);
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

        public Option<Actor> from() {
            return from;
        }

        public Object message() {
            return message;
        }
    }

    public static class PoisonPill {}
    
    public static class Kill {}
}