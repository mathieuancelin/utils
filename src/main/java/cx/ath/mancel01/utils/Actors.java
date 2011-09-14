/*
 *  Copyright 2011 Mathieu ANCELIN
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
package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.Option;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * Small Actors library for concurrent programming models.
 * 
 * @author Mathieu ANCELIN
 */
public final class Actors {

    private Actors() {}
    private static final ConcurrentHashMap<String, ActorRef> actors =
            new ConcurrentHashMap<String, ActorRef>();
    private static ServerBootstrap bootstrap;
    private static Channel channel;
    private static String host;
    private static String port;
    private static ExecutorService remotingServerExecutor = 
            Executors.newCachedThreadPool();
    private static ExecutorService bossServerExecutor = 
            Executors.newCachedThreadPool();
    private static ExecutorService clientServerExecutor = 
            Executors.newCachedThreadPool();
    private static ExecutorService clientBossServerExecutor = 
            Executors.newCachedThreadPool();

    private static ActorRef getActor(String name) {
        return actors.get(name);
    }

    private static void register(String name, ActorRef actor) {
        actors.putIfAbsent(name, actor);
    }

    private static void unregister(String name) {
        actors.remove(name);
    }

    public static Option<ActorRef> forName(String name) {
        return Option.maybe(Actors.getActor(name));
    }

    public static void shutdownAll() {
        try { Thread.sleep(50); } catch (Throwable t) {}
        Actor.executor.shutdown();
        Actor.executor.shutdownNow();
        Actor.executor = Executors.newCachedThreadPool();
    }

    public static void startRemoting(String host, int port) {
        Actors.host = host;
        Actors.port = String.valueOf(port);
        try {
            bootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                    remotingServerExecutor,
                    bossServerExecutor));
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(),
                        new RemoteActorHandler());
                }
            });
            channel = bootstrap.bind(new InetSocketAddress(host, port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopRemoting() {
        new Thread() {
            @Override
            public void run() {
                try {
                    channel.unbind().awaitUninterruptibly();
                } catch(Throwable t) {}
                try {
                    bootstrap.releaseExternalResources();
                } catch(Throwable t) {}
                try {
                    remotingServerExecutor.shutdown();
                    bossServerExecutor.shutdown();
                    clientServerExecutor.shutdown();
                    clientBossServerExecutor.shutdown();
                    remotingServerExecutor.shutdownNow();
                    bossServerExecutor.shutdownNow();
                    clientServerExecutor.shutdownNow();
                    clientBossServerExecutor.shutdownNow();
                } catch(Throwable t) {}
                remotingServerExecutor = Executors.newCachedThreadPool();
                bossServerExecutor = Executors.newCachedThreadPool();
                clientServerExecutor = Executors.newCachedThreadPool();
                clientBossServerExecutor = Executors.newCachedThreadPool();
            }
        }.start();
    }

    public static ActorStore remote(final String host, final int port) {
        return new ActorStore() {
            @Override
            public ActorRef forName(final String name) {
                return RemoteActorRef.ref(name, host, port);
            }
        };
    }

    public static interface ActorStore {

        ActorRef forName(String name);
    }

    public static interface ActorRef {
        
        String id();
        
        boolean buzy();

        void send(Object msg);

        void send(Object msg, String from);
        
        void send(Object msg, ActorURL from);

        ActorURL asLocalURL();

        ActorURL asRemoteURL();
    }

    public static interface LocalActorRef extends ActorRef {

        void send(Object msg, ActorRef from);
    }

    public static abstract class Actor implements Runnable, LocalActorRef {

        private static ExecutorService executor = Executors.newCachedThreadPool();

        static {
            Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            executor.shutdownNow();
                            executor.awaitTermination(3600, TimeUnit.SECONDS);
                            stopRemoting();
                        } catch (Throwable t) {
                            // nothing here
                        }
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
        private List<RemoteActorRef> refs = new ArrayList<RemoteActorRef>();
        private Action<Object> react;
        private AtomicBoolean started = new AtomicBoolean(false);
        private AtomicBoolean buzy = new AtomicBoolean(false);
        protected Option<ActorRef> sender = Option.none();
        protected final String uuid;
        private CountDownLatch latch = new CountDownLatch(1);

        public Actor() {
            uuid = UUID.randomUUID().toString();
            Actors.register(uuid, this);
        }

        @Override
        public boolean buzy() {
            return buzy.get();
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
        
        private void waitIfMailboxIsEmpty() {
            if (mailbox.isEmpty()) {
                setLatchAndWait();
            }
        }
        
        private void setLatchAndWait() {
            if (latch.getCount() == 0) {
                latch = new CountDownLatch(1);
            }
            try {
                latch.await();
            } catch (InterruptedException ex) {
                //ex.printStackTrace();
            }
        }

        /**
         * Method to loop the reception function to read all messages
         * in the mailbox one by one. 
         * Once launch, the loop will end with the stopActor method
         * or with a poison pill.
         */
        public final void loop(Action react) {
            this.react = react;
            while (started.get()) {
                waitIfMailboxIsEmpty();
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
        }

        /**
         * Read only one message in the mailbox with the function.
         */
        public final void react(Action<Object> react) {
            this.react = react;
            waitIfMailboxIsEmpty();
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
            if (msg != null) {
                mailbox.add(new Message(msg, Option.<ActorRef>none()));
                latch.countDown();
            }
        }

        @Override
        public final void send(Object msg, ActorRef from) {
            if (msg != null) {
                mailbox.add(new Message(msg, Option.maybe(from)));
                latch.countDown();
            }
        }

        @Override
        public final void send(Object msg, String from) {
            if (from.startsWith("remote://")) { //shouldn't happen               
                RemoteActorRef ref = RemoteActorRef.ref(ActorURL.fromString(from));
                if (!refs.contains(ref)) {
                    refs.add(ref);
                }
                send(msg, ref);
            } else {
                send(msg, Actors.getActor(from));
            }
        }  

        @Override
        public void send(Object msg, ActorURL from) {
            if (from.isRemote()) {
                RemoteActorRef ref = RemoteActorRef.ref(from);
                if (!refs.contains(ref)) {
                    refs.add(ref);
                }
                send(msg, ref);
            } else {
                send(msg, Actors.getActor(from.name));
            }
        }

        @Override
        public String id() {
            return uuid;
        }

        @Override
        public ActorURL asLocalURL() {
            return new LocalActorURL(Actors.host, Actors.port, uuid);
        }
        
        @Override
        public ActorURL asRemoteURL() {
            return new RemoteActorURL(Actors.host, Actors.port, uuid);
        }

        public final Actor stopActor() {
            started.compareAndSet(true, false);
            for (RemoteActorRef ref : refs) {
                //ref.close();
            }
            return this;
        }

        public final Actor startActor() {
            started.compareAndSet(false, true);
            executor.submit(this);
            return this;
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
        
        @Override
        public ActorURL asLocalURL() {
            return new LocalActorURL(Actors.host, Actors.port, name);
        }
        
        @Override
        public ActorURL asRemoteURL() {
            return new RemoteActorURL(Actors.host, Actors.port, name);
        }

        @Override
        public String id() {
            return name;
        }
    }
    
    public static class RemoteActorRef extends SimpleChannelUpstreamHandler implements ActorRef {

        private final String host;
        private final int port;
        private final String name;
        private ChannelFuture channel;
        private ClientBootstrap bootstrap;
        
        private static final ConcurrentHashMap<String, RemoteActorRef> refs = 
                new ConcurrentHashMap<String, RemoteActorRef>();
        
        public static RemoteActorRef ref(String name, String host, int port) {
            String key = name + host + port;
            if (!refs.containsKey(key)) {
                refs.putIfAbsent(key, new RemoteActorRef(name, host, port));
            }
            return refs.get(key);
        }
        
        public static RemoteActorRef ref(ActorURL url) {
            String key = url.name + url.host + url.port;
            if (!refs.containsKey(key)) {
                refs.putIfAbsent(key, new RemoteActorRef(url.name, url.host, url.port()));
            }
            return refs.get(key);
        }

        private RemoteActorRef(String name, String host, int port) {
            this.host = host;
            this.port = port;
            this.name = name;
            bootstrap = new ClientBootstrap(
                    new NioClientSocketChannelFactory(
                    clientServerExecutor,
                    clientBossServerExecutor));
            final SimpleChannelUpstreamHandler handler = this;
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() throws Exception {
                    return Channels.pipeline(
                        new ObjectEncoder(),
                        new ObjectDecoder(), handler);
                }
            });
            channel = bootstrap.connect(new InetSocketAddress(host, port));
        }

        @Override
        public void send(Object msg) {
            sendToRemoteActor(msg, "blackhole");
        }

        @Override
        public void send(Object msg, String from) {
            sendToRemoteActor(msg, from);
        }
        
        public void close() {
            channel.awaitUninterruptibly().getChannel().disconnect();
            bootstrap.releaseExternalResources();
        }
        
        private void sendToRemoteActor(Object msg, String from) {
            try {             
                RemoteActorMessage message = new RemoteActorMessage();
                message.remoteName = name;
                message.remoteMsg = msg;
                message.remoteFrom = from;
                message.remoteHost = Actors.host;
                message.remotePort = Actors.port;
                channel.awaitUninterruptibly().getChannel().write(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            SimpleLogger.error("RemoteActorRef exception : ", e.getCause());
            e.getChannel().close();
        }

        @Override
        public void send(Object msg, ActorURL from) {
            try {             
                RemoteActorMessage message = new RemoteActorMessage();
                message.remoteName = name;
                message.remoteMsg = msg;
                message.remoteFrom = from.name;
                message.remoteHost = from.host;
                message.remotePort = from.port;
                channel.awaitUninterruptibly().getChannel().write(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String id() {
            return new RemoteActorURL(host, String.valueOf(port), name).toString();
        }

        @Override
        public boolean buzy() {
            return false; // TODO : find a better way
        }

        @Override
        public ActorURL asLocalURL() {
            return new RemoteActorURL(host, String.valueOf(port), name);
        }

        @Override
        public ActorURL asRemoteURL() {
            return new RemoteActorURL(host, String.valueOf(port), name);
        }
    }
    
    public static class LoadBalancer {

        private final List<ActorRef> actors;
        private Iterator<ActorRef> it;

        public LoadBalancer(List<ActorRef> actors) {
            this.actors = actors;
            this.it = actors.iterator();
        }

        public final void send(Broadcast msg) {
            broadcast(msg.message, msg.from.getOrNull());
        }
        
        public final void send(Object msg) {
            chooseAndSend(msg, null);
        }

        public final void send(Object msg, ActorRef from) {
            chooseAndSend(msg, from);
        }

        private void chooseAndSend(Object msg, ActorRef from) {
            if (!it.hasNext()) {
                it = actors.iterator();
            }
            ActorRef a = it.next();
            if (!a.buzy()) {
                a.send(msg, from.id());
            } else {
                boolean sent = false;
                for (ActorRef bis : actors) {
                    if (!bis.buzy()) {
                        a.send(msg, from.id());
                        sent = true;
                    }
                }
                if (!sent) {
                    a.send(msg, from.id());
                }
            }
        }

        private void broadcast(Object message, ActorRef from) {
            for (ActorRef actor : actors) {
                if (actor instanceof Actor) {
                    ((Actor) actor).send(message, from);
                } else {
                    actor.send(message, from.id());
                }
            }
        }
    }
        
    public static class Broadcaster {

        private final List<ActorRef> actors;

        public Broadcaster(List<ActorRef> actors) {
            this.actors = actors;
        }

        public final void send(Object msg) {
            for (ActorRef actor : actors) {
                if (actor instanceof Actor) {
                    ((Actor) actor).send(msg);
                } else {
                    actor.send(msg);
                }
            }
        }

        public final void send(Object msg, ActorRef from) {
            for (ActorRef actor : actors) {
                if (actor instanceof Actor) {
                    ((Actor) actor).send(msg, from);
                } else {
                    actor.send(msg, from.id());
                }
            }
        }
    }

    public static class Broadcast implements Serializable {

        private final Object message;
        private final Option<ActorRef> from;

        public Broadcast(Object message, Option<ActorRef> from) {
            this.message = message;
            this.from = from;
        }

        public Broadcast(Object message) {
            this.message = message;
            this.from = Option.none();
        }

        public Option<ActorRef> from() {
            return from;
        }

        public Object message() {
            return message;
        }
    }

    public static class RemoteActorHandler extends SimpleChannelUpstreamHandler {

        @Override
        public void messageReceived(
                ChannelHandlerContext ctx, MessageEvent e) {
            if (e.getMessage() instanceof RemoteActorMessage) {
                RemoteActorMessage message = (RemoteActorMessage) e.getMessage();
                RemoteActorURL from = new RemoteActorURL(
                        message.remoteHost, message.remotePort, message.remoteFrom);
                Option<ActorRef> ref = forName(message.remoteName);
                if (ref.isDefined()) {
                    try {
                        ref.get().send(message.remoteMsg, from);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    SimpleLogger.error("{} actor isn't defined", message.remoteName);
                }
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            SimpleLogger.error("RemoteActorHandler exception : ", e.getCause());
            e.getChannel().close();
        }
    }

    private static class MessageDelivered implements Serializable {}

    private static class MessageUndelivered implements Serializable {}
    
    public static class PoisonPill implements Serializable {}

    public static class Kill implements Serializable {}

    private static class RemoteActorMessage implements Serializable {
        String remoteName;
        String remoteFrom;
        String remoteHost;
        String remotePort;
        Object remoteMsg;
    }
    
    public static abstract class ActorURL {
        
        public final String protocol;
        public final String host;
        public final String port;
        public final String name;

        public ActorURL(String protocol, String host, String port, String name) {
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.name = name;
        }
        
        public int port() {
            return Integer.valueOf(port);
        }

        @Override
        public String toString() {
            return protocol + "://" + host + ":" + port + "/" + name;
        }
        
        public abstract boolean isRemote();
        
        public static ActorURL fromString(String url) {
            if (url.startsWith("remote")) {
                url = url.replace("remote://", "");
                String host = url.split(":")[0];
                String rest = url.split(":")[1];
                String port = rest.split("/")[0];
                String name = rest.split("/")[1];
                return new RemoteActorURL(name, host, port);
            } else {
                url = url.replace("local://", "");
                String host = url.split(":")[0];
                String rest = url.split(":")[1];
                String port = rest.split("/")[0];
                String name = rest.split("/")[1];
                return new LocalActorURL(name, host, port);
            }
        }
    }
    
    public static class RemoteActorURL extends ActorURL {
        public RemoteActorURL(String host, String port, String name) {
            super("remote", host, port, name);
        }

        @Override
        public boolean isRemote() {
            return true;
        }
    }
    
    public static class LocalActorURL extends ActorURL {
        public LocalActorURL(String host, String port, String name) {
            super("local", host, port, name);
        }

        @Override
        public boolean isRemote() {
            return false;
        }
    }
}