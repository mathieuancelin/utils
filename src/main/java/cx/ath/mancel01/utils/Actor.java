package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.C.Function;
import cx.ath.mancel01.utils.F.Option;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Actor extends Thread {
    
    private static ExecutorService executor = Executors.newFixedThreadPool(4);
    
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

    @Override
    public void run() {
        mailbox.clear();
        started.compareAndSet(false, true);
        act();
        started.compareAndSet(true, false);
    }
    
    public abstract void act();
    
    public final void loop(Function react) {
        this.react = react;
        while(started.get()) {
            Message ret = mailbox.poll();
            if (ret != null) {
                sender = ret.sender;
                react.apply(ret.payload);
                sender = Option.none();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public final void react(Function<Object> react) {
        this.react = react;
        Message ret = mailbox.poll();
        if (ret != null) {
            sender = ret.sender;
            react.apply(ret.payload);
            sender = Option.none();
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
}
